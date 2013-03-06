/*
 * Copyright 2012 Midokura Europe SARL
 */

package org.midonet.cluster;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.junit.Before;
import org.junit.Test;

import org.midonet.midolman.Setup;
import org.midonet.midolman.config.MidolmanConfig;
import org.midonet.midolman.config.ZookeeperConfig;
import org.midonet.midolman.guice.MockMonitoringStoreModule;
import org.midonet.midolman.guice.cluster.DataClusterClientModule;
import org.midonet.midolman.guice.config.MockConfigProviderModule;
import org.midonet.midolman.guice.config.TypedConfigModule;
import org.midonet.midolman.guice.reactor.ReactorModule;
import org.midonet.midolman.guice.zookeeper.MockZookeeperConnectionModule;
import org.midonet.midolman.layer3.Route.NextHop;
import org.midonet.midolman.state.Directory;
import org.midonet.midolman.state.StateAccessException;
import org.midonet.midolman.state.zkManagers.RouteZkManager;
import org.midonet.midolman.state.zkManagers.RouterZkManager;
import org.midonet.cluster.data.Route;
import org.midonet.cluster.data.Router;
import org.midonet.cluster.data.ports.MaterializedRouterPort;
import org.midonet.packets.MAC;
import org.midonet.packets.Net;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class LocalDataClientImplTest {

    @Inject
    DataClient client;
    Injector injector = null;
    String zkRoot = "/test/v3/midolman";


    HierarchicalConfiguration fillConfig(HierarchicalConfiguration config) {
        config.addNodes(ZookeeperConfig.GROUP_NAME,
            Arrays.asList(new HierarchicalConfiguration.Node
                ("midolman_root_key", zkRoot)));
        return config;

    }

    RouteZkManager getRouteZkManager() {
        return injector.getInstance(RouteZkManager.class);
    }

    RouterZkManager getRouterZkManager() {
        return injector.getInstance(RouterZkManager.class);
    }

    Directory zkDir() {
        return injector.getInstance(Directory.class);
    }

    @Before
    public void initialize() throws InterruptedException, KeeperException {
        HierarchicalConfiguration config = fillConfig(
            new HierarchicalConfiguration());
        injector = Guice.createInjector(
            new MockConfigProviderModule(config),
            new MockZookeeperConnectionModule(),
            new TypedConfigModule<MidolmanConfig>(MidolmanConfig.class),

            new ReactorModule(),
            new MockMonitoringStoreModule(),
            new DataClusterClientModule()
        );
        injector.injectMembers(this);
        String[] nodes = zkRoot.split("/");
        String path = "/";

        for (String node : nodes) {
            if (!node.isEmpty()) {
                zkDir().add(path + node, null, CreateMode.PERSISTENT);
                path += node;
                path += "/";
            }
        }
        Setup.createZkDirectoryStructure(zkDir(), zkRoot);
    }

    @Test
    public void routerPortLifecycleTest() throws StateAccessException {
        // Create a materialized router port.
        UUID routerId = client.routersCreate(new Router());
        UUID portId = client.portsCreate(
            new MaterializedRouterPort().setDeviceId(routerId)
                .setHwAddr(MAC.fromString("02:BB:EE:EE:FF:01"))
                .setPortAddr("10.0.0.3").setNwAddr("10.0.0.0")
                .setNwLength(24)
        );
        // Verify that this automatically creates one route.
        List<Route> routes = client.routesFindByRouter(routerId);
        assertThat(routes, hasSize(1));
        Route rt = routes.get(0);
        // Verify that the route is type LOCAL and forwards to the new port.
        assertThat(rt.getNextHop(), equalTo(NextHop.LOCAL));
        assertThat(rt.getNextHopPort(), equalTo(portId));
        assertThat(rt.getNextHopGateway(), equalTo(
            Net.convertIntAddressToString(
                org.midonet.midolman.layer3.Route.NO_GATEWAY)));
        // Now delete the port and verify that the route is deleted.
        client.portsDelete(portId);
        routes = client.routesFindByRouter(routerId);
        assertThat(routes, hasSize(0));
    }
}