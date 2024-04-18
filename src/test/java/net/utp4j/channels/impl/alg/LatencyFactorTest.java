package net.utp4j.channels.impl.alg;

import net.utp4j.channels.impl.UtpTimestampedPacketDTO;
import net.utp4j.data.MicroSecondsTimeStamp;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LatencyFactorTest {

    @Test
    void testAckOnTime() throws SocketException {
        /* Intended Behaviour:
         *
         * Reception of three acks with a delay of exactly the RTT should
         * each increase the maximum window size by their proportion of the window size
         * multiplied with the maximum window gain
         */
        final int maxWindowIncrease = UtpAlgConfiguration.MAX_CWND_INCREASE_PACKETS_PER_RTT;

        // Setup algorithm
        MicroSecondsTimeStamp stamper = mock(MicroSecondsTimeStamp.class);
        when(stamper.timeStamp()).thenReturn(0L); // returns 1s
        UtpAlgorithm algorithm = new UtpAlgorithm(stamper, new InetSocketAddress(51235));
        algorithm.setEstimatedRtt(100);
        ByteBuffer bufferMock = ByteBuffer.allocate(10000);
        algorithm.setByteBuffer(bufferMock);

        OutPacketBuffer outBuffer = mock(OutPacketBuffer.class);
        // oldest pkt was sent as timestamp 0
        when(outBuffer.getOldestUnackedTimestamp()).thenReturn(0L);
        algorithm.setOutPacketBuffer(outBuffer);
        algorithm.setMaxWindow(4000); // maxWindow 4kB

        // mock on-fly packets: 3 packets sent at timestamp 0 of 1kB each
        for (int i = 1; i < 4; i++) {
            UtpTimestampedPacketDTO pkt = PacketTestUtil.createPacket(i, 1000);
            algorithm.markPacketOnfly(pkt.utpPacket(), pkt.dataGram());
        }

        // mock method for returning size of pkt
        when(outBuffer.markPacketAcked(anyInt(), anyLong(), anyBoolean())).thenReturn(1000);

        /* Start of Test
         * Situation: 3 unacked packets are on the network
         * When the acks for these packets arrive without any delay, they should maximally increase the window
         * */
        long originalWindowSize = algorithm.getMaxWindow();
        byte[] selAck = {(byte) 0, (byte) 0, (byte) 0, (byte) 0};
        when(stamper.timeStamp()).thenReturn(100000L); // receive ack 0.1s after packet was sent: exactly RT

        // ack 1
        UtpTimestampedPacketDTO ackPacket = PacketTestUtil.createSelAckPacket(1, selAck);
        algorithm.ackRecieved(ackPacket);

        long windowSizeAfterFirstPacket = algorithm.getMaxWindow();


        // ack 2

        ackPacket = PacketTestUtil.createSelAckPacket(2, selAck);
        algorithm.ackRecieved(ackPacket);

        long windowSizeAfterSecondPacket = algorithm.getMaxWindow();

        // ack 3

        ackPacket = PacketTestUtil.createSelAckPacket(2, selAck);
        algorithm.ackRecieved(ackPacket);

        long windowSizeAfterThirdPacket = algorithm.getMaxWindow();

        // Verify the three acks had the intended effect on the congestion window

        // since packet 1 was received with perfect latency and takes up 1/4 of the total window, we expect to grow
        // the maxWindow by a quarter of the maximum gain per RTT
        assertEquals(originalWindowSize + maxWindowIncrease / 4, windowSizeAfterFirstPacket);

        // packet 2 takes up 1/4.75 of the total window, we expect to grow by 1/4.75 * maxWindowIncrease
        assertEquals(windowSizeAfterFirstPacket + (int) (maxWindowIncrease / 4.75), windowSizeAfterSecondPacket);

        // etc
        assertEquals(windowSizeAfterSecondPacket + (int) (maxWindowIncrease * (1000.0 / windowSizeAfterSecondPacket)), windowSizeAfterThirdPacket);
    }

    @Test
    void testAckWithinTargetLatency() throws SocketException {
        /* Intended Behaviour:
         *
         * Reception of three acks with an increasing delay, but still within the target delay
         * each increase the maximum window size by their proportion of the window size
         * multiplied with the maximum window gain
         */
        final int maxWindowIncrease = UtpAlgConfiguration.MAX_CWND_INCREASE_PACKETS_PER_RTT;

        // Setup algorithm
        MicroSecondsTimeStamp stamper = mock(MicroSecondsTimeStamp.class);
        when(stamper.timeStamp()).thenReturn(0L); // returns 1s
        UtpAlgorithm algorithm = new UtpAlgorithm(stamper, new InetSocketAddress(51235));
        algorithm.setEstimatedRtt(100);
        ByteBuffer bufferMock = ByteBuffer.allocate(10000);
        algorithm.setByteBuffer(bufferMock);

        OutPacketBuffer outBuffer = mock(OutPacketBuffer.class);
        // oldest pkt was sent as timestamp 0
        when(outBuffer.getOldestUnackedTimestamp()).thenReturn(0L);
        algorithm.setOutPacketBuffer(outBuffer);
        algorithm.setMaxWindow(4000); // maxWindow 4kB

        // mock on-fly packets: 3 packets sent at timestamp 0 of 1kB each
        for (int i = 1; i < 4; i++) {
            UtpTimestampedPacketDTO pkt = PacketTestUtil.createPacket(i, 1000);
            algorithm.markPacketOnfly(pkt.utpPacket(), pkt.dataGram());
        }

        // mock method for returning size of pkt
        when(outBuffer.markPacketAcked(anyInt(), anyLong(), anyBoolean())).thenReturn(1000);

        /* Start of Test
         * Situation: 3 unacked packets are on the network
         * When acks arrive with progressively bigger delays, the resulting gain should get smaller
         * */
        long originalWindowSize = algorithm.getMaxWindow();
        byte[] selAck = {(byte) 0, (byte) 0, (byte) 0, (byte) 0};

        // ack 1
        when(stamper.timeStamp()).thenReturn(100000L); // receive ack 0.1s after packet was sent: exactly RT
        UtpTimestampedPacketDTO ackPacket = PacketTestUtil.createSelAckPacket(1, selAck);
        ackPacket.utpPacket().setTimestampDifference(100000);
        algorithm.ackRecieved(ackPacket);

        long gainAfterFirstPacket = algorithm.getMaxWindow() - originalWindowSize;

        // reset congestion window for consistency
        algorithm.setMaxWindow(4000); // maxWindow 4kB

        // ack 2
        // receive ack at 0.1s + 0.25 * congestion target
        when(stamper.timeStamp()).thenReturn(100000L + (long) (0.25 * UtpAlgConfiguration.C_CONTROL_TARGET_MICROS));
        ackPacket = PacketTestUtil.createSelAckPacket(2, selAck);
        ackPacket.utpPacket().setTimestampDifference(100000 + (int) (0.25 * UtpAlgConfiguration.C_CONTROL_TARGET_MICROS));
        algorithm.ackRecieved(ackPacket);

        long gainAfterSecondPacket = algorithm.getMaxWindow() - originalWindowSize;
        // reset congestion window for consistency
        algorithm.setMaxWindow(4000); // maxWindow 4kB

        // ack 3
        // receive ack at 0.1s + 0.75 * congestion target
        when(stamper.timeStamp()).thenReturn(100000L + (long) (0.75 * UtpAlgConfiguration.C_CONTROL_TARGET_MICROS));
        ackPacket = PacketTestUtil.createSelAckPacket(3, selAck);
        ackPacket.utpPacket().setTimestampDifference(100000 + (int) (0.75 * UtpAlgConfiguration.C_CONTROL_TARGET_MICROS));
        algorithm.ackRecieved(ackPacket);

        long gainAfterThirdPacket = algorithm.getMaxWindow() - originalWindowSize;

        // Verify the three acks had the intended effect on the congestion window

        // since packet 1 was received with perfect latency and takes up 1/4 of the total window, we expect the gain
        // to be a quarter of the maximum gain per RTT
        assertEquals(maxWindowIncrease / 4, gainAfterFirstPacket);

        // packet 2 has a delay of 25% of the congestion control target delay, so should have about 75% of the gain of the first packet
        assertEquals(0.75 * gainAfterFirstPacket, gainAfterSecondPacket, 10);

        // packet 3 has a delay of 75% of the congestion control target delay, so should have only 25% of the gain of the first packet
        assertEquals(0.25 * gainAfterFirstPacket, gainAfterThirdPacket, 10);
    }

    @Test
    void testAckOverTargetLatency() throws SocketException {
        /* Intended Behaviour:
         *
         * Reception of three acks with an increasing delay, with the first ack received at RTT and the second and third
         * acks received with a delay greater than the target delay
         * The last 2 packets lead to a negative gain in congestion window size
         */
        final int maxWindowIncrease = UtpAlgConfiguration.MAX_CWND_INCREASE_PACKETS_PER_RTT;

        // Setup algorithm
        MicroSecondsTimeStamp stamper = mock(MicroSecondsTimeStamp.class);
        when(stamper.timeStamp()).thenReturn(0L); // returns 1s
        UtpAlgorithm algorithm = new UtpAlgorithm(stamper, new InetSocketAddress(51235));
        algorithm.setEstimatedRtt(100);
        ByteBuffer bufferMock = ByteBuffer.allocate(10000);
        algorithm.setByteBuffer(bufferMock);

        OutPacketBuffer outBuffer = mock(OutPacketBuffer.class);
        // oldest pkt was sent as timestamp 0
        when(outBuffer.getOldestUnackedTimestamp()).thenReturn(0L);
        algorithm.setOutPacketBuffer(outBuffer);
        algorithm.setMaxWindow(4000); // maxWindow 4kB

        // mock on-fly packets: 3 packets sent at timestamp 0 of 1kB each
        for (int i = 1; i < 4; i++) {
            UtpTimestampedPacketDTO pkt = PacketTestUtil.createPacket(i, 1000);
            algorithm.markPacketOnfly(pkt.utpPacket(), pkt.dataGram());
        }

        // mock method for returning size of pkt
        when(outBuffer.markPacketAcked(anyInt(), anyLong(), anyBoolean())).thenReturn(1000);

        /* Start of Test
         * Situation: 3 unacked packets are on the network
         * When acks arrive with progressively bigger delays, the resulting gain should get smaller
         * */
        long originalWindowSize = algorithm.getMaxWindow();
        byte[] selAck = {(byte) 0, (byte) 0, (byte) 0, (byte) 0};

        // ack 1
        when(stamper.timeStamp()).thenReturn(100000L); // receive ack 0.1s after packet was sent: exactly RTT
        UtpTimestampedPacketDTO ackPacket = PacketTestUtil.createSelAckPacket(1, selAck);
        ackPacket.utpPacket().setTimestampDifference(100000);
        algorithm.ackRecieved(ackPacket);

        long gainAfterFirstPacket = algorithm.getMaxWindow() - originalWindowSize;

        // reset congestion window for consistency
        algorithm.setMaxWindow(4000); // maxWindow 4kB

        // ack 2
        // receive ack at 0.1s + 1.25 * congestion target
        when(stamper.timeStamp()).thenReturn(100000L + (long) (1.25 * UtpAlgConfiguration.C_CONTROL_TARGET_MICROS));
        ackPacket = PacketTestUtil.createSelAckPacket(2, selAck);
        ackPacket.utpPacket().setTimestampDifference(100000 + (int) (1.25 * UtpAlgConfiguration.C_CONTROL_TARGET_MICROS));
        algorithm.ackRecieved(ackPacket);

        long gainAfterSecondPacket = algorithm.getMaxWindow() - originalWindowSize;
        // reset congestion window for consistency
        algorithm.setMaxWindow(4000); // maxWindow 4kB

        // ack 3
        // receive ack at 0.1s + 1.75 * congestion target
        when(stamper.timeStamp()).thenReturn(100000L + (long) (1.75 * UtpAlgConfiguration.C_CONTROL_TARGET_MICROS));
        ackPacket = PacketTestUtil.createSelAckPacket(3, selAck);
        ackPacket.utpPacket().setTimestampDifference(100000 + (int) (1.75 * UtpAlgConfiguration.C_CONTROL_TARGET_MICROS));
        algorithm.ackRecieved(ackPacket);

        long gainAfterThirdPacket = algorithm.getMaxWindow() - originalWindowSize;

        // Verify the three acks had the intended effect on the congestion window

        // since packet 1 was received with perfect latency and takes up 1/4 of the total window, we expect the gain
        // to be a quarter of the maximum gain per RTT
        assertEquals(maxWindowIncrease / 4, gainAfterFirstPacket);

        // packet 2 has a delay of 125% of the congestion control target delay, so should have about -25% of the gain of the first packet
        assertEquals(-0.25 * gainAfterFirstPacket, gainAfterSecondPacket, 10);

        // packet 3 has a delay of 175% of the congestion control target delay, so should have only -75% of the gain of the first packet
        assertEquals(-0.75 * gainAfterFirstPacket, gainAfterThirdPacket, 10);
    }
}
