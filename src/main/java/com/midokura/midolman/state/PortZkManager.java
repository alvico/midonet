/*
 * @(#)PortZkManager        1.6 11/09/08
 *
 * Copyright 2011 Midokura KK
 */
package com.midokura.midolman.state;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

import com.midokura.midolman.layer3.Route;
import com.midokura.midolman.state.RouterZkManager.PeerRouterConfig;
import com.midokura.midolman.util.ShortUUID;

/**
 * Class to manage the port ZooKeeper data.
 * 
 * @version 1.6 08 Sept 2011
 * @author Ryu Ishimoto
 */
public class PortZkManager extends ZkManager {

    /**
     * Initializes a PortZkManager object with a ZooKeeper client and the root
     * path of the ZooKeeper directory.
     * 
     * @param zk
     *            Directory object.
     * @param basePath
     *            The root path.
     */
    public PortZkManager(Directory zk, String basePath) {
        super(zk, basePath);
    }

    public PortZkManager(ZooKeeper zk, String basePath) {
        this(new ZkDirectory(zk, "", null), basePath);
    }

    /**
     * Constructs a list of ZooKeeper update operations to perform when adding a
     * new port.
     * 
     * @param entry
     *            ZooKeeper node representing a key-value entry of port UUID and
     *            PortConfig object.
     * @return A list of Op objects to represent the operations to perform.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     */
    public List<Op> preparePortCreate(
            ZkNodeEntry<UUID, PortDirectory.PortConfig> portNode)
            throws ZkStateSerializationException {

        List<Op> ops = new ArrayList<Op>();
        try {
            ops.add(Op.create(pathManager.getPortPath(portNode.key),
                    serialize(portNode.value), Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT));
        } catch (IOException e) {
            throw new ZkStateSerializationException(
                    "Could not serialize PortConfig", e,
                    PortDirectory.PortConfig.class);
        }

        if (portNode.value instanceof PortDirectory.RouterPortConfig) {
            ops.add(Op.create(pathManager.getRouterPortPath(
                    portNode.value.device_id, portNode.key), null,
                    Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
            ops.add(Op.create(pathManager.getPortRoutesPath(portNode.key),
                    null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));

            if (portNode.value instanceof PortDirectory.MaterializedRouterPortConfig) {
                ops.add(Op.create(pathManager.getPortBgpPath(portNode.key),
                        null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
            }
        } else if (portNode.value instanceof PortDirectory.BridgePortConfig) {
            ops.add(Op.create(pathManager.getBridgePortPath(
                    portNode.value.device_id, portNode.key), null,
                    Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        } else {
            throw new IllegalArgumentException("Unrecognized port type.");
        }

        return ops;
    }

    private List<Op> prepareCommonRouterPortDelete(
            ZkNodeEntry<UUID, PortDirectory.PortConfig> entry)
            throws KeeperException, InterruptedException,
            ZkStateSerializationException {
        List<Op> ops = new ArrayList<Op>();
        RouteZkManager routeZkManager = new RouteZkManager(zk, pathManager
                .getBasePath());
        List<ZkNodeEntry<UUID, Route>> routes = routeZkManager.listPortRoutes(
                entry.key, null);
        for (ZkNodeEntry<UUID, Route> route : routes) {
            ops.addAll(routeZkManager.prepareRouteDelete(route));
        }
        ops.add(Op.delete(pathManager.getRouterPortPath(entry.value.device_id,
                entry.key), -1));
        ops.add(Op.delete(pathManager.getPortPath(entry.key), -1));
        return ops;
    }

    public List<Op> preparePortCreateLink(
            ZkNodeEntry<UUID, PortDirectory.PortConfig> localPortEntry,
            ZkNodeEntry<UUID, PortDirectory.PortConfig> peerPortEntry)
            throws ZkStateSerializationException {
        List<Op> ops = new ArrayList<Op>();
        ops.addAll(preparePortCreate(localPortEntry));
        ops.addAll(preparePortCreate(peerPortEntry));

        PeerRouterConfig peerRouter = new PeerRouterConfig(localPortEntry.key,
                peerPortEntry.key);
        PeerRouterConfig localRouter = new PeerRouterConfig(peerPortEntry.key,
                localPortEntry.key);
        try {
            ops.add(Op.create(pathManager.getRouterRouterPath(
                    localPortEntry.value.device_id,
                    peerPortEntry.value.device_id), serialize(peerRouter),
                    Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
            ops.add(Op.create(pathManager.getRouterRouterPath(
                    peerPortEntry.value.device_id,
                    localPortEntry.value.device_id), serialize(localRouter),
                    Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT));
        } catch (IOException e) {
            throw new ZkStateSerializationException(
                    "Could not deserialize peer routers to PeerRouterConfig",
                    e, PeerRouterConfig.class);
        }

        return ops;
    }

    /**
     * Constructs a list of operations to perform in a router port deletion.
     * 
     * @param entry
     *            Port ZooKeeper entry to delete.
     * @return A list of Op objects representing the operations to perform.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public List<Op> prepareRouterPortDelete(
            ZkNodeEntry<UUID, PortDirectory.PortConfig> entry)
            throws KeeperException, InterruptedException,
            ZkStateSerializationException {
        List<Op> ops = prepareCommonRouterPortDelete(entry);

        if (entry.value instanceof PortDirectory.LogicalRouterPortConfig) {
            UUID peerPortId = ((PortDirectory.LogicalRouterPortConfig) entry.value).peer_uuid;
            // For logical router ports, we need to delete the peer Port.
            if (peerPortId != null) {
                ZkNodeEntry<UUID, PortDirectory.PortConfig> peerPortEntry = get(peerPortId);
                if (peerPortEntry != null) {
                    ops.addAll(prepareCommonRouterPortDelete(peerPortEntry));
                    // Remove the peer router associations
                    ops.add(Op.delete(pathManager.getRouterRouterPath(
                            entry.value.device_id,
                            peerPortEntry.value.device_id), -1));
                    ops.add(Op.delete(pathManager.getRouterRouterPath(
                            peerPortEntry.value.device_id,
                            entry.value.device_id), -1));
                }
            }
        }
        return ops;
    }

    /**
     * Constructs a list of operations to perform in a bridge port deletion.
     * 
     * @param entry
     *            Port ZooKeeper entry to delete.
     * @return A list of Op objects representing the operations to perform.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public List<Op> prepareBridgePortDelete(
            ZkNodeEntry<UUID, PortDirectory.PortConfig> entry)
            throws KeeperException, InterruptedException {
        List<Op> ops = new ArrayList<Op>();
        ops.add(Op.delete(pathManager.getBridgePortPath(entry.value.device_id,
                entry.key), -1));
        ops.add(Op.delete(pathManager.getPortPath(entry.key), -1));
        return ops;
    }

    public List<Op> preparePortDelete(UUID id)
            throws KeeperException, InterruptedException,
            ZkStateSerializationException {
        return preparePortDelete(get(id));
    }
    
    /**
     * Constructs a list of operations to perform in a port deletion.
     * 
     * @param entry
     *            Port ZooKeeper entry to delete.
     * @return A list of Op objects representing the operations to perform.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public List<Op> preparePortDelete(
            ZkNodeEntry<UUID, PortDirectory.PortConfig> entry)
            throws KeeperException, InterruptedException,
            ZkStateSerializationException {
        if (entry.value instanceof PortDirectory.BridgePortConfig) {
            return prepareBridgePortDelete(entry);
        } else {
            return prepareRouterPortDelete(entry);
        }
    }

    /**
     * Performs an atomic update on the ZooKeeper to add a new port entry.
     * 
     * @param route
     *            PortConfig object to add to the ZooKeeper directory.
     * @return The UUID of the newly created object.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public UUID create(PortDirectory.PortConfig port) throws IOException,
            KeeperException, InterruptedException,
            ZkStateSerializationException {
        // TODO(pino) - port UUIDs should be created using a sequential
        // persistent
        // create in a ZK directory.
        UUID id = ShortUUID.generate32BitUUID();
        ZkNodeEntry<UUID, PortDirectory.PortConfig> portNode = new ZkNodeEntry<UUID, PortDirectory.PortConfig>(
                id, port);
        zk.multi(preparePortCreate(portNode));
        return id;
    }

    public ZkNodeEntry<UUID, UUID> createLink(
            PortDirectory.LogicalRouterPortConfig localPort,
            PortDirectory.LogicalRouterPortConfig peerPort)
            throws KeeperException, InterruptedException,
            ZkStateSerializationException {
        // Check that they are not currently linked.
        if (zk.has(pathManager.getRouterRouterPath(localPort.device_id,
                peerPort.device_id))) {
            throw new IllegalArgumentException(
                    "Invalid connection.  The router ports are already connected.");
        }
        localPort.peer_uuid = ShortUUID.generate32BitUUID();
        peerPort.peer_uuid = ShortUUID.generate32BitUUID();

        ZkNodeEntry<UUID, PortDirectory.PortConfig> localPortEntry = new ZkNodeEntry<UUID, PortDirectory.PortConfig>(
                peerPort.peer_uuid, localPort);
        ZkNodeEntry<UUID, PortDirectory.PortConfig> peerPortEntry = new ZkNodeEntry<UUID, PortDirectory.PortConfig>(
                localPort.peer_uuid, peerPort);
        zk.multi(preparePortCreateLink(localPortEntry, peerPortEntry));
        return new ZkNodeEntry<UUID, UUID>(peerPort.peer_uuid,
                localPort.peer_uuid);
    }

    /**
     * Gets a ZooKeeper node entry key-value pair of a port with the given ID.
     * 
     * @param id
     *            The ID of the port.
     * @return Port object found.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public ZkNodeEntry<UUID, PortDirectory.PortConfig> get(UUID id)
            throws KeeperException, InterruptedException,
            ZkStateSerializationException {
        return get(id, null);
    }

    /**
     * Gets a ZooKeeper node entry key-value pair of a port with the given ID
     * and sets a watcher on the node.
     * 
     * @param id
     *            The ID of the port.
     * @param watcher
     *            The watcher that gets notified when there is a change in the
     *            node.
     * @return Route object found.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public ZkNodeEntry<UUID, PortDirectory.PortConfig> get(UUID id,
            Runnable watcher) throws KeeperException, InterruptedException,
            ZkStateSerializationException {
        byte[] data = zk.get(pathManager.getPortPath(id), watcher);
        PortDirectory.PortConfig config = null;
        try {
            config = deserialize(data, PortDirectory.PortConfig.class);
        } catch (IOException e) {
            throw new ZkStateSerializationException(
                    "Could not deserialize port " + id + " to PortConfig", e,
                    PortDirectory.PortConfig.class);
        }
        return new ZkNodeEntry<UUID, PortDirectory.PortConfig>(id, config);
    }

    /**
     * Gets a list of ZooKeeper port nodes belonging under the directory path
     * specified.
     * 
     * @param path
     *            The directory path of the parent node.
     * @param watcher
     *            The watcher to set on the changes to the ports for this port.
     * @return A list of ZooKeeper port nodes.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public List<ZkNodeEntry<UUID, PortDirectory.PortConfig>> listPorts(
            String path, Runnable watcher) throws KeeperException,
            InterruptedException, ZkStateSerializationException {
        List<ZkNodeEntry<UUID, PortDirectory.PortConfig>> result = new ArrayList<ZkNodeEntry<UUID, PortDirectory.PortConfig>>();
        Set<String> portIds = zk.getChildren(path, watcher);
        for (String portId : portIds) {
            // For now, get each one.
            result.add(get(UUID.fromString(portId)));
        }
        return result;
    }

    /**
     * Gets a list of ZooKeeper port nodes belonging to a router with the given
     * ID.
     * 
     * @param routerId
     *            The ID of the router to find the ports of.
     * @return A list of ZooKeeper port nodes.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public List<ZkNodeEntry<UUID, PortDirectory.PortConfig>> listRouterPorts(
            UUID routerId) throws KeeperException, InterruptedException,
            ZkStateSerializationException {
        return listRouterPorts(routerId, null);
    }

    /**
     * Gets a list of ZooKeeper port nodes belonging to a router with the given
     * ID.
     * 
     * @param routerId
     *            The ID of the router to find the ports of.
     * @param watcher
     *            The watcher to set on the changes to the ports for this
     *            router.
     * @return A list of ZooKeeper route nodes.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public List<ZkNodeEntry<UUID, PortDirectory.PortConfig>> listRouterPorts(
            UUID routerId, Runnable watcher) throws KeeperException,
            InterruptedException, ZkStateSerializationException {
        return listPorts(pathManager.getRouterPortsPath(routerId), watcher);
    }

    /**
     * Gets a list of ZooKeeper port nodes belonging to a bridge with the given
     * ID.
     * 
     * @param bridgeId
     *            The ID of the bridge to find the ports of.
     * @return A list of ZooKeeper port nodes.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public List<ZkNodeEntry<UUID, PortDirectory.PortConfig>> listBridgePorts(
            UUID bridgeId) throws KeeperException, InterruptedException,
            ZkStateSerializationException {
        return listBridgePorts(bridgeId, null);
    }

    /**
     * Gets a list of ZooKeeper port nodes belonging to a bridge with the given
     * ID.
     * 
     * @param bridgeId
     *            The ID of the bridge to find the routes of.
     * @param watcher
     *            The watcher to set on the changes to the ports for this
     *            router.
     * @return A list of ZooKeeper port nodes.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public List<ZkNodeEntry<UUID, PortDirectory.PortConfig>> listBridgePorts(
            UUID bridgeId, Runnable watcher) throws KeeperException,
            InterruptedException, ZkStateSerializationException {
        return listPorts(pathManager.getBridgePortsPath(bridgeId), watcher);
    }

    /**
     * Updates the PortConfig values with the given PortConfig object.
     * 
     * @param entry
     *            PortConfig object to save.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public void update(ZkNodeEntry<UUID, PortDirectory.PortConfig> entry)
            throws KeeperException, InterruptedException,
            ZkStateSerializationException {
        // Update any version for now.
        try {
            zk.update(pathManager.getPortPath(entry.key),
                    serialize(entry.value));

        } catch (IOException e) {
            throw new ZkStateSerializationException("Could not serialize port "
                    + entry.key + " to PortConfig", e,
                    PortDirectory.PortConfig.class);
        }
    }

    /***
     * Deletes a port and its related data from the ZooKeeper directories
     * atomically.
     * 
     * @param id
     *            ID of the port to delete.
     * @throws ZkStateSerializationException
     *             Serialization error occurred.
     * @throws KeeperException
     *             ZooKeeper error occurred.
     * @throws InterruptedException
     *             ZooKeeper was unresponsive.
     */
    public void delete(UUID id) throws InterruptedException, KeeperException,
            ZkStateSerializationException {
        this.zk.multi(preparePortDelete(id));
    }

}
