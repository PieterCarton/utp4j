package net.utp4j.channels.impl.alg;

import net.utp4j.channels.impl.UtpTimestampedPacketDTO;
import net.utp4j.data.MicroSecondsTimeStamp;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PacketTimeoutTest {
    @Test
    public void testTimeoutHighJitter() throws SocketException {
        /* Scenario:
         * In a normal situation, we expect packets to time out after a delay of RTT + 4 * RTT_VAR
         *
         * Here, we send 10 packets, and we receive 8 acks with a jitter of 10ms.
         * A 9th ack is then received with a delay of RTT + 2*RTT_VAR, which should not cause a time-out
         * A 10th ack is then received with a delay of RTT + 6*RTT_VAR, which should cause a time-out
         */

        UtpAlgConfiguration.MINIMUM_TIMEOUT_MILLIS = 115;
        MicroSecondsTimeStamp stamper = mock(MicroSecondsTimeStamp.class);
        when(stamper.timeStamp()).thenReturn(1000000L); // returns 1s
        UtpAlgorithm algorithm = new UtpAlgorithm(stamper,  new InetSocketAddress(51235));
        algorithm.setEstimatedRtt(100);

        OutPacketBuffer outBuffer = new OutPacketBuffer(stamper);
        outBuffer.setRemoteAdress(new InetSocketAddress(51235));
        algorithm.setByteBuffer(ByteBuffer.allocate(200000));
        algorithm.setMaxWindow(10000);
        algorithm.setCurrentWindow(0);
        algorithm.setOutPacketBuffer(outBuffer);

        // in the scenario, 10 packets were sent in a burst at t=10ms
        when(outBuffer.getOldestUnackedTimestamp()).thenReturn(10000L);
        for (int i = 1; i <= 10; i++) {
            UtpTimestampedPacketDTO pkt = PacketTestUtil.createPacket(i, 1000);
            algorithm.markPacketOnfly(pkt.utpPacket(), pkt.dataGram());
        }



        byte[] selAck = {(byte) 0, (byte) 0, (byte) 0, (byte) 0};
        // the first 8 acks are received 10ms after RTT
        for (int i = 1; i <= 8; i++) {
            // receive acks at t=120ms
            when(stamper.timeStamp()).thenReturn(120000L);
            UtpTimestampedPacketDTO ackPacket = PacketTestUtil.createSelAckPacket(i, selAck);
            ackPacket.utpPacket().setTimestampDifference(110);
            algorithm.ackRecieved(ackPacket);
        }

        // at this point, the rtt and rtt_var should be 103ms and 4ms
        long rtt = 103000;
        long rtt_var = 4000;

        // at t=113ms+2*4ms=121ms (2*rtt_var behind retransmission time), no packets should time out yet
        assertEquals(0, algorithm.getPacketsToResend().size());

        // the 9th ack, t=121ms
        when(stamper.timeStamp()).thenReturn(121000L);
        UtpTimestampedPacketDTO ackPacket = PacketTestUtil.createSelAckPacket(9, selAck);
        ackPacket.utpPacket().setTimestampDifference(108);
        algorithm.ackRecieved(ackPacket);

        // at t=10ms+103ms+6*4ms (6*rtt_var behind retransmission time), the 10th packets should have timed out
        when(stamper.timeStamp()).thenReturn(137000L);
        assertEquals(1, algorithm.getPacketsToResend().size());

        UtpAlgConfiguration.MINIMUM_TIMEOUT_MILLIS = 500;
    }

    @Test
    public void testTimeoutLowJitter() throws SocketException {
        /* Scenario:
         * In cases of very stable latency, the rttVar might be estimated to be 0ms
         * As a result, UTP4j might timeout packets even if the ack is only received a fraction
         * of a millisecond too late.
         *
         * This is only an issue for very high latencies, as there is normally a minimum time-out in
         * place of 500ms. The issue does become more apparent for a latency of 500ms (and a corresponding RTT of 1000ms).
         *
         * It is debatable whether this issue can be seen as a bug, due to the unrealistic network conditions needed to
         * make it take effect. However, it could be fixed by setting a lower bound for the estimated round trip time variance.
         * For example, setting the variance to at least 1% of the round trip time.
         */

        UtpAlgConfiguration.MINIMUM_TIMEOUT_MILLIS = 500;
        MicroSecondsTimeStamp stamper = mock(MicroSecondsTimeStamp.class);
        when(stamper.timeStamp()).thenReturn(1000000L); // returns 1s
        UtpAlgorithm algorithm = new UtpAlgorithm(stamper,  new InetSocketAddress(51235));
        algorithm.setEstimatedRtt(1000);

        OutPacketBuffer outBuffer = new OutPacketBuffer(stamper);
        outBuffer.setRemoteAdress(new InetSocketAddress(51235));
        algorithm.setByteBuffer(ByteBuffer.allocate(200000));
        algorithm.setCurrentWindow(20000);
        algorithm.setOutPacketBuffer(outBuffer);

        algorithm.setMaxWindow(10000); // window is half full

        // in the scenario, 10 packets were sent in a burst at t=10ms
        when(outBuffer.getOldestUnackedTimestamp()).thenReturn(10000L);
        for (int i = 1; i <= 10; i++) {
            UtpTimestampedPacketDTO pkt = PacketTestUtil.createPacket(i, 1000);
            algorithm.markPacketOnfly(pkt.utpPacket(), pkt.dataGram());
        }
        // after 10 packets of 1kB, we have 10kB on-fly
//        when(outBuffer.getBytesOnfly()).thenReturn(0); // 20 kB onfly

        // the first 9 acks are received with increasing delays, but still within the roundtip time

        byte[] selAck = {(byte) 0, (byte) 0, (byte) 0, (byte) 0};

        for (int i = 1; i <= 9; i++) {
            // jitter is sub-millisecond, and therefore does not increase the calculated RTT variance
            when(stamper.timeStamp()).thenReturn(1009000L + 100L * i);
            UtpTimestampedPacketDTO ackPacket = PacketTestUtil.createSelAckPacket(i, selAck);
            ackPacket.utpPacket().setTimestampDifference(10000);
            algorithm.ackRecieved(ackPacket);
        }

        // the 10th ack is timed out 1/10th of a millisecond after RTT
        when(stamper.timeStamp()).thenReturn(1010100L);
        assertEquals(1, algorithm.getPacketsToResend().size());
    }
}
