/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.net.module.util.netlink;

import static android.system.OsConstants.NETLINK_ROUTE;

import static com.android.net.module.util.netlink.StructNdMsg.NUD_STALE;
import static com.android.testutils.NetlinkTestUtils.makeDelNeighMessage;
import static com.android.testutils.NetlinkTestUtils.makeNewNeighMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.net.InetAddresses;
import android.system.OsConstants;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import libcore.util.HexEncoding;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RtNetlinkNeighborMessageTest {
    private static final String TAG = "RtNetlinkNeighborMessageTest";

    public static final byte[] RTM_DELNEIGH = makeDelNeighMessage(
            InetAddresses.parseNumericAddress("192.168.159.254"), NUD_STALE);

    public static final byte[] RTM_NEWNEIGH = makeNewNeighMessage(
            InetAddresses.parseNumericAddress("fe80::86c9:b2ff:fe6a:ed4b"), NUD_STALE);

    // An example of the full response from an RTM_GETNEIGH query.
    private static final String RTM_GETNEIGH_RESPONSE_HEX =
            // <-- struct nlmsghr             -->|<-- struct ndmsg           -->|<-- struct nlattr: NDA_DST             -->|<-- NDA_LLADDR          -->|<-- NDA_PROBES -->|<-- NDA_CACHEINFO                         -->|
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 15000000 4000 00 05 1400 0100 ff020000000000000000000000000001 0a00 0200 333300000001 0000 0800 0400 00000000 1400 0300 a2280000 32110000 32110000 01000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 15000000 4000 00 05 1400 0100 ff0200000000000000000001ff000001 0a00 0200 3333ff000001 0000 0800 0400 00000000 1400 0300 0d280000 9d100000 9d100000 00000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 15000000 0400 80 01 1400 0100 20010db800040ca00000000000000001 0a00 0200 84c9b26aed4b 0000 0800 0400 04000000 1400 0300 90100000 90100000 90080000 01000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 15000000 4000 00 05 1400 0100 ff0200000000000000000001ff47da19 0a00 0200 3333ff47da19 0000 0800 0400 00000000 1400 0300 a1280000 31110000 31110000 01000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 14000000 4000 00 05 1400 0100 ff020000000000000000000000000016 0a00 0200 333300000016 0000 0800 0400 00000000 1400 0300 912a0000 21130000 21130000 00000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 14000000 4000 00 05 1400 0100 ff0200000000000000000001ffeace3b 0a00 0200 3333ffeace3b 0000 0800 0400 00000000 1400 0300 922a0000 22130000 22130000 00000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 15000000 4000 00 05 1400 0100 ff0200000000000000000001ff5c2a83 0a00 0200 3333ff5c2a83 0000 0800 0400 00000000 1400 0300 391c0000 c9040000 c9040000 01000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 01000000 4000 00 02 1400 0100 00000000000000000000000000000000 0a00 0200 000000000000 0000 0800 0400 00000000 1400 0300 cd180200 5d010200 5d010200 08000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 15000000 4000 00 05 1400 0100 ff020000000000000000000000000002 0a00 0200 333300000002 0000 0800 0400 00000000 1400 0300 352a0000 c5120000 c5120000 00000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 15000000 4000 00 05 1400 0100 ff020000000000000000000000000016 0a00 0200 333300000016 0000 0800 0400 00000000 1400 0300 982a0000 28130000 28130000 00000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 15000000 0800 80 01 1400 0100 fe8000000000000086c9b2fffe6aed4b 0a00 0200 84c9b26aed4b 0000 0800 0400 00000000 1400 0300 23000000 24000000 57000000 13000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 15000000 4000 00 05 1400 0100 ff0200000000000000000001ffeace3b 0a00 0200 3333ffeace3b 0000 0800 0400 00000000 1400 0300 992a0000 29130000 29130000 01000000" +
            "58000000 1c00 0200 00000000 3e2b0000 0a 00 0000 14000000 4000 00 05 1400 0100 ff020000000000000000000000000002 0a00 0200 333300000002 0000 0800 0400 00000000 1400 0300 2e2a0000 be120000 be120000 00000000" +
            "44000000 1c00 0200 00000000 3e2b0000 02 00 0000 18000000 4000 00 03 0800 0100 00000000                         0400 0200                   0800 0400 00000000 1400 0300 75280000 05110000 05110000 22000000";
    public static final byte[] RTM_GETNEIGH_RESPONSE =
            HexEncoding.decode(RTM_GETNEIGH_RESPONSE_HEX.replaceAll(" ", "").toCharArray(), false);

    @Test
    public void testParseRtmDelNeigh() {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(RTM_DELNEIGH);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);  // For testing.
        final NetlinkMessage msg = NetlinkMessage.parse(byteBuffer, NETLINK_ROUTE);
        assertNotNull(msg);
        assertTrue(msg instanceof RtNetlinkNeighborMessage);
        final RtNetlinkNeighborMessage neighMsg = (RtNetlinkNeighborMessage) msg;

        final StructNlMsgHdr hdr = neighMsg.getHeader();
        assertNotNull(hdr);
        assertEquals(76, hdr.nlmsg_len);
        assertEquals(NetlinkConstants.RTM_DELNEIGH, hdr.nlmsg_type);
        assertEquals(0, hdr.nlmsg_flags);
        assertEquals(0, hdr.nlmsg_seq);
        assertEquals(0, hdr.nlmsg_pid);

        final StructNdMsg ndmsgHdr = neighMsg.getNdHeader();
        assertNotNull(ndmsgHdr);
        assertEquals((byte) OsConstants.AF_INET, ndmsgHdr.ndm_family);
        assertEquals(21, ndmsgHdr.ndm_ifindex);
        assertEquals(NUD_STALE, ndmsgHdr.ndm_state);
        final InetAddress destination = neighMsg.getDestination();
        assertNotNull(destination);
        assertEquals(InetAddress.parseNumericAddress("192.168.159.254"), destination);
    }

    @Test
    public void testParseRtmNewNeigh() {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(RTM_NEWNEIGH);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);  // For testing.
        final NetlinkMessage msg = NetlinkMessage.parse(byteBuffer, NETLINK_ROUTE);
        assertNotNull(msg);
        assertTrue(msg instanceof RtNetlinkNeighborMessage);
        final RtNetlinkNeighborMessage neighMsg = (RtNetlinkNeighborMessage) msg;

        final StructNlMsgHdr hdr = neighMsg.getHeader();
        assertNotNull(hdr);
        assertEquals(88, hdr.nlmsg_len);
        assertEquals(NetlinkConstants.RTM_NEWNEIGH, hdr.nlmsg_type);
        assertEquals(0, hdr.nlmsg_flags);
        assertEquals(0, hdr.nlmsg_seq);
        assertEquals(0, hdr.nlmsg_pid);

        final StructNdMsg ndmsgHdr = neighMsg.getNdHeader();
        assertNotNull(ndmsgHdr);
        assertEquals((byte) OsConstants.AF_INET6, ndmsgHdr.ndm_family);
        assertEquals(21, ndmsgHdr.ndm_ifindex);
        assertEquals(NUD_STALE, ndmsgHdr.ndm_state);
        final InetAddress destination = neighMsg.getDestination();
        assertNotNull(destination);
        assertEquals(InetAddress.parseNumericAddress("fe80::86c9:b2ff:fe6a:ed4b"), destination);
    }

    @Test
    public void testParseRtmGetNeighResponse() {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(RTM_GETNEIGH_RESPONSE);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);  // For testing.

        int messageCount = 0;
        while (byteBuffer.remaining() > 0) {
            final NetlinkMessage msg = NetlinkMessage.parse(byteBuffer, NETLINK_ROUTE);
            assertNotNull(msg);
            assertTrue(msg instanceof RtNetlinkNeighborMessage);
            final RtNetlinkNeighborMessage neighMsg = (RtNetlinkNeighborMessage) msg;

            final StructNlMsgHdr hdr = neighMsg.getHeader();
            assertNotNull(hdr);
            assertEquals(NetlinkConstants.RTM_NEWNEIGH, hdr.nlmsg_type);
            assertEquals(StructNlMsgHdr.NLM_F_MULTI, hdr.nlmsg_flags);
            assertEquals(0, hdr.nlmsg_seq);
            assertEquals(11070, hdr.nlmsg_pid);

            final int probes = neighMsg.getProbes();
            assertTrue("Unexpected number of probes. Got " +  probes + ", max=5",
                    probes < 5);
            final int ndm_refcnt = neighMsg.getCacheInfo().ndm_refcnt;
            assertTrue("nda_cacheinfo has unexpectedly high ndm_refcnt: " + ndm_refcnt,
                    ndm_refcnt < 0x100);

            messageCount++;
        }
        // TODO: add more detailed spot checks.
        assertEquals(14, messageCount);
    }

    @Test
    public void testCreateRtmNewNeighMessage() {
        final int seqNo = 2635;
        final int ifIndex = 14;
        final byte[] llAddr =
                new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6 };

        // Hexadecimal representation of our created packet.
        final String expectedNewNeighHex =
                // struct nlmsghdr
                "30000000" +     // length = 48
                "1c00" +         // type = 28 (RTM_NEWNEIGH)
                "0501" +         // flags (NLM_F_REQUEST | NLM_F_ACK | NLM_F_REPLACE)
                "4b0a0000" +     // seqno
                "00000000" +     // pid (0 == kernel)
                // struct ndmsg
                "02" +           // family
                "00" +           // pad1
                "0000" +         // pad2
                "0e000000" +     // interface index (14)
                "0800" +         // NUD state (0x08 == NUD_DELAY)
                "00" +           // flags
                "00" +           // type
                // struct nlattr: NDA_DST
                "0800" +         // length = 8
                "0100" +         // type (1 == NDA_DST, for neighbor messages)
                "7f000001" +     // IPv4 address (== 127.0.0.1)
                // struct nlattr: NDA_LLADDR
                "0a00" +         // length = 10
                "0200" +         // type (2 == NDA_LLADDR, for neighbor messages)
                "010203040506" + // MAC Address (== 01:02:03:04:05:06)
                "0000";          // padding, for 4 byte alignment
        final byte[] expectedNewNeigh =
                HexEncoding.decode(expectedNewNeighHex.toCharArray(), false);

        final byte[] bytes = RtNetlinkNeighborMessage.newNewNeighborMessage(
            seqNo, Inet4Address.LOOPBACK, StructNdMsg.NUD_DELAY, ifIndex, llAddr);
        if (!Arrays.equals(expectedNewNeigh, bytes)) {
            assertEquals(expectedNewNeigh.length, bytes.length);
            for (int i = 0; i < Math.min(expectedNewNeigh.length, bytes.length); i++) {
                assertEquals(expectedNewNeigh[i], bytes[i]);
            }
        }
    }
}
