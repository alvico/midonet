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
package org.midonet.odp.protos;

import org.junit.Before;
import org.junit.Test;
import org.midonet.odp.Datapath;
import org.midonet.odp.Flow;
import org.midonet.odp.FlowMatch;
import org.midonet.odp.flows.FlowKey;
import org.midonet.odp.flows.FlowKeyEtherType;
import org.midonet.packets.IPv4Addr;
import org.midonet.packets.MAC;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.midonet.odp.flows.FlowKeys.*;

public class OvsFlowsDeleteTest extends AbstractNetlinkProtocolTest {

    @Before
    public void setUp() throws Exception {
        super.setUp(responses);
        setConnection();
        connection.bypassSendQueue(true);
        connection.setMaxBatchIoOps(1);
    }

    @Test
    public void testFlowsDelete() throws Exception {

        initializeConnection(connection.futures.initialize(), 6);

        Future<Datapath> dpResult = connection.futures.datapathsGet("bibi");
        // multi containing the datapaths data
        exchangeMessage();

        Datapath datapath = dpResult.get();

        assertThat("connection.datapathsGet() returned null",
                   datapath, notNullValue());

        Future<Flow> flowResult =
            connection.futures.flowsCreate(datapath, new Flow(flowMatch()));

        // multi containing the ports data
        exchangeMessage();

        Flow flow = flowResult.get();
        assertThat("The created flow has the same keySet as the requested one",
                   flow.getMatch(), equalTo(flowMatch()));

        Future<Flow> deleteFlowOp =
            connection.futures.flowsDelete(datapath, flow.getMatch().getKeys());
        exchangeMessage();

        assertThat("The created flow has the same keySet as the requested one",
                   deleteFlowOp.get().getMatch(),
                   equalTo(flowMatch()));

        Future<Set<Flow>> enumeratedFlows =
            connection.futures.flowsEnumerate(datapath);

        exchangeMessage();

        assertThat("The flowsFlush() result should have been true",
                   enumeratedFlows.get(), hasSize(0));
    }

    private static FlowMatch flowMatch() {
        return new FlowMatch()
            .addKey(inPort(0))
            .addKey(ethernet(MAC.fromString("ae:b3:77:8c:a1:48").getAddress(),
                    MAC.fromString("33:33:00:00:00:16").getAddress()))
            .addKey(etherType(FlowKeyEtherType.Type.ETH_P_8021Q))
            .addKey(vlan((short) 0x0101))
            .addKey(
                encap(Arrays.<FlowKey>asList(
                    etherType(FlowKeyEtherType.Type.ETH_P_ARP),
                    arp(MAC.fromString("ae:b3:77:8d:c1:48").getAddress(),
                            MAC.fromString("ae:b3:70:8d:c1:48").getAddress(),
                            (short) 2,
                            IPv4Addr.stringToInt("192.168.100.1"),
                            IPv4Addr.stringToInt("192.168.102.1")))));
    }

    final byte[][] responses = {
/*
// write - time: 1345715977293
    {
        (byte)0x28, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
        (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0x11, (byte)0x00, (byte)0x02, (byte)0x00,
        (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x64, (byte)0x61,
        (byte)0x74, (byte)0x61, (byte)0x70, (byte)0x61, (byte)0x74, (byte)0x68,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    },
*/
        // read - time: 1345715977303
        {
            (byte)0xC0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x02,
            (byte)0x00, (byte)0x00, (byte)0x11, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x64, (byte)0x61,
            (byte)0x74, (byte)0x61, (byte)0x70, (byte)0x61, (byte)0x74, (byte)0x68,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x06, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x01, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x04, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x05, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x54, (byte)0x00, (byte)0x06, (byte)0x00, (byte)0x14, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x14, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x14, (byte)0x00, (byte)0x03, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x03, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x0E, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x14, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x24, (byte)0x00, (byte)0x07, (byte)0x00, (byte)0x20, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x11, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F,
            (byte)0x64, (byte)0x61, (byte)0x74, (byte)0x61, (byte)0x70, (byte)0x61,
            (byte)0x74, (byte)0x68, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        },
/*
// write - time: 1345715977306
    {
        (byte)0x24, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
        (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0x0E, (byte)0x00, (byte)0x02, (byte)0x00,
        (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x76, (byte)0x70,
        (byte)0x6F, (byte)0x72, (byte)0x74, (byte)0x00, (byte)0x00, (byte)0x00
    },
*/
        // read - time: 1345715977307
        {
            (byte)0xB8, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x02,
            (byte)0x00, (byte)0x00, (byte)0x0E, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x76, (byte)0x70,
            (byte)0x6F, (byte)0x72, (byte)0x74, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x06, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x09, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x03, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x64, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x54, (byte)0x00, (byte)0x06, (byte)0x00,
            (byte)0x14, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x14, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x0B, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x14, (byte)0x00,
            (byte)0x03, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x0E, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x14, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x07, (byte)0x00,
            (byte)0x1C, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x0E, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x6F, (byte)0x76,
            (byte)0x73, (byte)0x5F, (byte)0x76, (byte)0x70, (byte)0x6F, (byte)0x72,
            (byte)0x74, (byte)0x00, (byte)0x00, (byte)0x00
        },
/*
// write - time: 1345715977308
    {
        (byte)0x24, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
        (byte)0x01, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0x0D, (byte)0x00, (byte)0x02, (byte)0x00,
        (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x66, (byte)0x6C,
        (byte)0x6F, (byte)0x77, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    },
*/
        // read - time: 1345715977308
        {
            (byte)0xB8, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x02,
            (byte)0x00, (byte)0x00, (byte)0x0D, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x66, (byte)0x6C,
            (byte)0x6F, (byte)0x77, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x06, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x0A, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x03, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x06, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x54, (byte)0x00, (byte)0x06, (byte)0x00,
            (byte)0x14, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x14, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x0B, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x14, (byte)0x00,
            (byte)0x03, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x0E, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x14, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x07, (byte)0x00,
            (byte)0x1C, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x0D, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x6F, (byte)0x76,
            (byte)0x73, (byte)0x5F, (byte)0x66, (byte)0x6C, (byte)0x6F, (byte)0x77,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        },
/*
// write - time: 1345715977309
    {
        (byte)0x24, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
        (byte)0x01, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0x0F, (byte)0x00, (byte)0x02, (byte)0x00,
        (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x70, (byte)0x61,
        (byte)0x63, (byte)0x6B, (byte)0x65, (byte)0x74, (byte)0x00, (byte)0x00
    },
*/
        // read - time: 1345715977309
        {
            (byte)0x5C, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x02,
            (byte)0x00, (byte)0x00, (byte)0x0F, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x70, (byte)0x61,
            (byte)0x63, (byte)0x6B, (byte)0x65, (byte)0x74, (byte)0x00, (byte)0x00,
            (byte)0x06, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x0B, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x03, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x04, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x18, (byte)0x00, (byte)0x06, (byte)0x00,
            (byte)0x14, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00,
            (byte)0x00, (byte)0x00
        },
/*
// write - time: 1345715977310
    {
        (byte)0x28, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
        (byte)0x01, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0x11, (byte)0x00, (byte)0x02, (byte)0x00,
        (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x64, (byte)0x61,
        (byte)0x74, (byte)0x61, (byte)0x70, (byte)0x61, (byte)0x74, (byte)0x68,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    },
*/
        // read - time: 1345715977311
        {
            (byte)0xC0, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x02,
            (byte)0x00, (byte)0x00, (byte)0x11, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x64, (byte)0x61,
            (byte)0x74, (byte)0x61, (byte)0x70, (byte)0x61, (byte)0x74, (byte)0x68,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x06, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x01, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x04, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x05, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x54, (byte)0x00, (byte)0x06, (byte)0x00, (byte)0x14, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x14, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x14, (byte)0x00, (byte)0x03, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x03, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x0E, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x14, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x24, (byte)0x00, (byte)0x07, (byte)0x00, (byte)0x20, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x11, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F,
            (byte)0x64, (byte)0x61, (byte)0x74, (byte)0x61, (byte)0x70, (byte)0x61,
            (byte)0x74, (byte)0x68, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        },
/*
// write - time: 1345715977313
    {
        (byte)0x24, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
        (byte)0x01, (byte)0x00, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0x0E, (byte)0x00, (byte)0x02, (byte)0x00,
        (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x76, (byte)0x70,
        (byte)0x6F, (byte)0x72, (byte)0x74, (byte)0x00, (byte)0x00, (byte)0x00
    },
*/
        // read - time: 1345715977313
        {
            (byte)0xB8, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x02,
            (byte)0x00, (byte)0x00, (byte)0x0E, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x6F, (byte)0x76, (byte)0x73, (byte)0x5F, (byte)0x76, (byte)0x70,
            (byte)0x6F, (byte)0x72, (byte)0x74, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x06, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x09, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x03, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x64, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x54, (byte)0x00, (byte)0x06, (byte)0x00,
            (byte)0x14, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x14, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00,
            (byte)0x0B, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x14, (byte)0x00,
            (byte)0x03, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x0E, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x14, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x01, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x08, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x0B, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x07, (byte)0x00,
            (byte)0x1C, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x0E, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x6F, (byte)0x76,
            (byte)0x73, (byte)0x5F, (byte)0x76, (byte)0x70, (byte)0x6F, (byte)0x72,
            (byte)0x74, (byte)0x00, (byte)0x00, (byte)0x00
        },
/*
// write - time: 1345715977339
    {
        (byte)0x24, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x01,
        (byte)0x09, (byte)0x00, (byte)0x07, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x09, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x62, (byte)0x69,
        (byte)0x62, (byte)0x69, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
    },
*/
        // read - time: 1345715977340
        {
            (byte)0x48, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0x07, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0xEA, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x09, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x62, (byte)0x69,
            (byte)0x62, (byte)0x69, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x24, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00
        },
/*
// write - time: 1345715977354
    {
        (byte)0x70, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0A, (byte)0x01,
        (byte)0x09, (byte)0x04, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0xEA, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x04, (byte)0x00, (byte)0x02, (byte)0x80, (byte)0x54, (byte)0x00,
        (byte)0x01, (byte)0x80, (byte)0x08, (byte)0x00, (byte)0x03, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00,
        (byte)0x04, (byte)0x00, (byte)0xAE, (byte)0xB3, (byte)0x77, (byte)0x8C,
        (byte)0xA1, (byte)0x48, (byte)0x33, (byte)0x33, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x16, (byte)0x06, (byte)0x00, (byte)0x06, (byte)0x00,
        (byte)0x81, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x06, (byte)0x00,
        (byte)0x05, (byte)0x00, (byte)0x11, (byte)0x01, (byte)0x00, (byte)0x00,
        (byte)0x28, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x06, (byte)0x00,
        (byte)0x06, (byte)0x00, (byte)0x08, (byte)0x06, (byte)0x00, (byte)0x00,
        (byte)0x1C, (byte)0x00, (byte)0x0D, (byte)0x00, (byte)0xC0, (byte)0xA8,
        (byte)0x64, (byte)0x01, (byte)0xC0, (byte)0xA8, (byte)0x66, (byte)0x01,
        (byte)0x00, (byte)0x02, (byte)0xAE, (byte)0xB3, (byte)0x77, (byte)0x8D,
        (byte)0xC1, (byte)0x48, (byte)0xAE, (byte)0xB3, (byte)0x70, (byte)0x8D,
        (byte)0xC1, (byte)0x48, (byte)0x00, (byte)0x00
    },
*/
        // read - time: 1345715977357
        {
            (byte)0x70, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0A, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0xEA, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x54, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x10, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0xAE, (byte)0xB3,
            (byte)0x77, (byte)0x8C, (byte)0xA1, (byte)0x48, (byte)0x33, (byte)0x33,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x16, (byte)0x06, (byte)0x00,
            (byte)0x06, (byte)0x00, (byte)0x81, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x06, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x11, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0x28, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x06, (byte)0x00, (byte)0x06, (byte)0x00, (byte)0x08, (byte)0x06,
            (byte)0x00, (byte)0x00, (byte)0x1C, (byte)0x00, (byte)0x0D, (byte)0x00,
            (byte)0xC0, (byte)0xA8, (byte)0x64, (byte)0x01, (byte)0xC0, (byte)0xA8,
            (byte)0x66, (byte)0x01, (byte)0x00, (byte)0x02, (byte)0xAE, (byte)0xB3,
            (byte)0x77, (byte)0x8D, (byte)0xC1, (byte)0x48, (byte)0xAE, (byte)0xB3,
            (byte)0x70, (byte)0x8D, (byte)0xC1, (byte)0x48, (byte)0x00, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x02, (byte)0x00
        },
/*
// write - time: 1345715977364
    {
        (byte)0x6C, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0A, (byte)0x01,
        (byte)0x09, (byte)0x00, (byte)0x09, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0xEA, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x54, (byte)0x00, (byte)0x01, (byte)0x80, (byte)0x08, (byte)0x00,
        (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x10, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0xAE, (byte)0xB3,
        (byte)0x77, (byte)0x8C, (byte)0xA1, (byte)0x48, (byte)0x33, (byte)0x33,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x16, (byte)0x06, (byte)0x00,
        (byte)0x06, (byte)0x00, (byte)0x81, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x06, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x11, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0x28, (byte)0x00, (byte)0x01, (byte)0x00,
        (byte)0x06, (byte)0x00, (byte)0x06, (byte)0x00, (byte)0x08, (byte)0x06,
        (byte)0x00, (byte)0x00, (byte)0x1C, (byte)0x00, (byte)0x0D, (byte)0x00,
        (byte)0xC0, (byte)0xA8, (byte)0x64, (byte)0x01, (byte)0xC0, (byte)0xA8,
        (byte)0x66, (byte)0x01, (byte)0x00, (byte)0x02, (byte)0xAE, (byte)0xB3,
        (byte)0x77, (byte)0x8D, (byte)0xC1, (byte)0x48, (byte)0xAE, (byte)0xB3,
        (byte)0x70, (byte)0x8D, (byte)0xC1, (byte)0x48, (byte)0x00, (byte)0x00
    },
*/
        // read - time: 1345715977365
        {
            (byte)0x70, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0A, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0x09, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0xEA, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x54, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x08, (byte)0x00,
            (byte)0x03, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x10, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0xAE, (byte)0xB3,
            (byte)0x77, (byte)0x8C, (byte)0xA1, (byte)0x48, (byte)0x33, (byte)0x33,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x16, (byte)0x06, (byte)0x00,
            (byte)0x06, (byte)0x00, (byte)0x81, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x06, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x11, (byte)0x01,
            (byte)0x00, (byte)0x00, (byte)0x28, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0x06, (byte)0x00, (byte)0x06, (byte)0x00, (byte)0x08, (byte)0x06,
            (byte)0x00, (byte)0x00, (byte)0x1C, (byte)0x00, (byte)0x0D, (byte)0x00,
            (byte)0xC0, (byte)0xA8, (byte)0x64, (byte)0x01, (byte)0xC0, (byte)0xA8,
            (byte)0x66, (byte)0x01, (byte)0x00, (byte)0x02, (byte)0xAE, (byte)0xB3,
            (byte)0x77, (byte)0x8D, (byte)0xC1, (byte)0x48, (byte)0xAE, (byte)0xB3,
            (byte)0x70, (byte)0x8D, (byte)0xC1, (byte)0x48, (byte)0x00, (byte)0x00,
            (byte)0x04, (byte)0x00, (byte)0x02, (byte)0x00
        },
/*
// write - time: 1345715977367
    {
        (byte)0x18, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0A, (byte)0x01,
        (byte)0x0D, (byte)0x06, (byte)0x0A, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x01,
        (byte)0x00, (byte)0x00, (byte)0xEA, (byte)0x00, (byte)0x00, (byte)0x00
    },
*/
        // read - time: 1345715977367
        {
            (byte)0x14, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x0A, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xA5, (byte)0x19, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00
        },
    };
}
