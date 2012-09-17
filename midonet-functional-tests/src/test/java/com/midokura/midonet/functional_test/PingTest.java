/*
 * Copyright 2011 Midokura Europe SARL
 */

package com.midokura.midonet.functional_test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.midokura.midonet.client.MidonetMgmt;
import com.midokura.midonet.client.resource.Host;
import com.midokura.midonet.client.resource.HostInterfacePort;
import com.midokura.midonet.client.resource.ResourceCollection;
import com.midokura.midonet.client.resource.Router;
import com.midokura.midonet.client.resource.RouterPort;
import com.midokura.midonet.functional_test.mocks.MockMgmtStarter;
import com.midokura.midonet.functional_test.utils.MidolmanLauncher;
import com.midokura.midonet.functional_test.utils.TapWrapper;
import com.midokura.packets.IntIPv4;
import com.midokura.packets.MAC;
import com.midokura.packets.MalformedPacketException;
import com.midokura.util.lock.LockHelper;


import static com.midokura.midonet.functional_test.FunctionalTestsHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class PingTest {

    private final static Logger log = LoggerFactory.getLogger(PingTest.class);

    IntIPv4 rtrIp = IntIPv4.fromString("192.168.231.1");
    IntIPv4 ip1 = IntIPv4.fromString("192.168.231.2");
    IntIPv4 ip3 = IntIPv4.fromString("192.168.231.4");
    final String TENANT_NAME = "tenant-ping";

    RouterPort p1;
    RouterPort p3;
    TapWrapper tap1;
    PacketHelper helper1;
    MidolmanLauncher midolman;
    MockMgmtStarter apiStarter;
    MidonetMgmt apiClient;

    static LockHelper.Lock lock;
    private static final String TEST_HOST_ID = "910de343-c39b-4933-86c7-540225fb02f9" ;

    @BeforeClass
    public static void checkLock() {
        lock = LockHelper.lock(FunctionalTestsHelper.LOCK_NAME);
    }

    @AfterClass
    public static void releaseLock() {
        lock.release();
    }

    @Before
    public void setUp() throws Exception {

        //fixQuaggaFolderPermissions();

        startCassandra();

        midolman = MidolmanLauncher.start("PingTest");

        apiStarter = new MockMgmtStarter(false);

        apiClient = new MidonetMgmt(apiStarter.getURI());

        log.debug("Building router");
        Router rtr = apiClient.addRouter().tenantId(TENANT_NAME).name("rtr1").create();
        log.debug("Router done!: " + rtr.getName());
        p1 = rtr.addMaterializedRouterPort().portAddress(ip1.toString());

        ResourceCollection<Host> hosts = apiClient.getHosts();

        Host host = null;

        for (Host h : hosts) {
            if (h.getId().toString().matches(TEST_HOST_ID)) {
                host = h;
            }
        }

        // check that we've actually found the test host.
        assertNotNull(host);

        tap1 = new TapWrapper("pingTestTap1");

        HostInterfacePort interfacePort = host.addHostInterfacePort()
                .interfaceName(tap1.getName())
                .portId(p1.getId());


        p3 = rtr.addMaterializedRouterPort().portAddress(ip3.toString());

        helper1 = new PacketHelper(MAC.fromString("02:00:00:aa:aa:01"), ip1, rtrIp);

        log.debug("Waiting for the systems to start properly.");
    }

    @After
    public void tearDown() throws Exception {
        removeTapWrapper(tap1);

        stopMidolman(midolman);
        stopMidolmanMgmt(apiStarter);
        stopCassandra();
        cleanupZooKeeperServiceData();
    }

    @Test
    public void testArpResolutionAndPortPing()
            throws MalformedPacketException, InterruptedException {
        byte[] request;

        // First arp for router's mac.
        assertThat("The ARP request was sent properly",
                tap1.send(helper1.makeArpRequest()));

        MAC rtrMac = helper1.checkArpReply(tap1.recv());
        helper1.setGwMac(rtrMac);


        // Ping router's port.
        request = helper1.makeIcmpEchoRequest(rtrIp);
        assertThat(String.format("The tap %s should have sent the packet", tap1.getName()),
            tap1.send(request));

        // Note: Midolman's virtual router currently does not ARP before
        // responding to ICMP echo requests addressed to its own port.
        PacketHelper.checkIcmpEchoReply(request, tap1.recv());

        // Ping internal port p3.
        request = helper1.makeIcmpEchoRequest(ip3);
        assertThat("The tap should have sent the packet again",
                tap1.send(request));
        // Note: the virtual router ARPs before delivering the reply packet.
        helper1.checkArpRequest(tap1.recv());
        assertThat("The tap should have sent the packet again",
                tap1.send(helper1.makeArpReply()));
        // Finally, the icmp echo reply from the peer.
        PacketHelper.checkIcmpEchoReply(request, tap1.recv());

        assertNoMorePacketsOnTap(tap1);
    }
}