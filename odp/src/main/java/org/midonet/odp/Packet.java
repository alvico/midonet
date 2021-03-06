/*
 * Copyright 2014 Midokura SARL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.midonet.odp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.midonet.netlink.NetlinkMessage;
import org.midonet.netlink.AttributeHandler;
import org.midonet.odp.OpenVSwitch.Packet.Attr;
import org.midonet.odp.flows.FlowAction;
import org.midonet.odp.flows.FlowActions;
import org.midonet.odp.flows.FlowKey;
import org.midonet.odp.flows.FlowKeys;
import org.midonet.packets.Ethernet;

/**
 * An abstraction over the Ovs kernel datapath Packet entity. Contains an
 * {@link FlowMatch} object and a <code>byte[] data</code> member when triggered
 * via a kernel notification.
 *
 * @see FlowMatch
 * @see OvsDatapathConnection#packetsExecute(Datapath, Packet)
 * @see OvsDatapathConnection#datapathsSetNotificationHandler(Datapath, Callback)
 */
public class Packet implements AttributeHandler {
    private static final Logger log = LoggerFactory
            .getLogger("org.midonet.netlink.netlink-proto");

    public enum Reason {
        FlowTableMiss,
        FlowActionUserspace,
    }

    private FlowMatch match = new FlowMatch();
    private Long userData;
    private Reason reason;
    private Ethernet eth;

    // user field used by midolman packet pipeline to track time statistics,
    // ignored in equals() and hashCode()
    public long startTimeNanos = 0;

    private Packet() { } // for deserialisation only

    public Packet(Ethernet eth) {
        this.eth = eth;
    }

    public Packet(Ethernet eth, FlowMatch match) {
        this.eth = eth;
        this.match = match;
    }

    public Ethernet getEthernet() {
        return eth;
    }

    public byte[] getData() {
        return eth.serialize();
    }

    public FlowMatch getMatch() {
        return match;
    }

    public Packet addKey(FlowKey key) {
        match.addKey(key);
        return this;
    }

    public Long getUserData() {
        return userData;
    }

    public Reason getReason() {
        return reason;
    }

    public Packet setReason(Reason reason) {
        this.reason = reason;
        return this;
    }

    public void processUserspaceKeys() {
        FlowMatches.addUserspaceKeys(eth, match);
    }

    public void generateFlowKeysFromPayload() {
        match = FlowMatches.fromEthernetPacket(eth);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        @SuppressWarnings("unchecked")
        Packet that = (Packet) o;

        return Objects.equals(this.eth, that.eth)
            && Objects.equals(this.match, that.match)
            && Objects.equals(this.userData, that.userData)
            && (this.reason == that.reason);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(eth);
        result = 31 * result + Objects.hashCode(match);
        result = 31 * result + Objects.hashCode(userData);
        result = 31 * result + Objects.hashCode(reason);
        return result;
    }


    @Override
    public String toString() {
        return "Packet{" +
            "data=" + eth +
            ", match=" + match +
            ", userData=" + userData +
            ", reason=" + reason +
            '}';
    }

    public static Packet fromEthernet(Ethernet eth) {
        return new Packet(eth);
    }

    public static Packet buildFrom(ByteBuffer buf) {
        int datapathIndex = buf.getInt(); // ignored
        Packet packet = new Packet();
        NetlinkMessage.scanAttributes(buf, packet);
        if (packet.eth == null)
            return null;
        return packet;
    }

    public void use(ByteBuffer buf, short id) {
        switch(NetlinkMessage.unnest(id)) {

            case Attr.Packet:
                ByteOrder originalOrder = buf.order();
                try {
                    this.eth = new Ethernet();
                    this.eth.deserialize(buf);
                } catch (Exception e) {
                    log.warn("Dropping malformed packet", e);
                    this.eth = null;
                } finally {
                    buf.order(originalOrder);
                }
                break;

            case Attr.Key:
                this.match = FlowMatch.reader.deserializeFrom(buf);
                break;

            case Attr.Userdata:
                this.userData = buf.getLong();
                break;
        }
    }

    /** Prepares an ovs request for executing and a packet with the given list
     *  of actions. */
    public static ByteBuffer execRequest(ByteBuffer buf, int datapathId,
                                         Iterable<FlowKey> keys,
                                         Iterable<FlowAction> actions,
                                         Ethernet packet) {
        buf.putInt(datapathId);
        // TODO(pino): find out why ovs_packet_cmd_execute throws an
        // EINVAL if we put the PACKET attribute right after the
        // datapathId. I examined the ByteBuffers constructed with that
        // ordering of attributes and compared it to this one, and found
        // only the expected difference.

        NetlinkMessage.writeAttrSeq(buf, Attr.Key, keys, FlowKeys.writer);

        NetlinkMessage.writeAttrSeq(buf, Attr.Actions,
                                    actions, FlowActions.writer);

        NetlinkMessage.writeRawAttribute(buf, Attr.Packet, packet.serialize());

        buf.flip();
        return buf;
    }
}
