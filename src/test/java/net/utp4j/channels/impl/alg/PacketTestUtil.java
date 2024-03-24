package net.utp4j.channels.impl.alg;

import net.utp4j.channels.impl.UtpTimestampedPacketDTO;
import net.utp4j.data.SelectiveAckHeaderExtension;
import net.utp4j.data.UtpPacket;
import net.utp4j.data.UtpPacketUtils;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import static net.utp4j.data.bytes.UnsignedTypesUtil.longToUshort;

public class PacketTestUtil {
    public static UtpTimestampedPacketDTO createPacket(int sequenceNumber, int packetLength) throws SocketException {
        UtpPacket pkt = new UtpPacket();
        pkt.setSequenceNumber(longToUshort(sequenceNumber));
        pkt.setPayload(new byte[packetLength]);
        SocketAddress addr = new InetSocketAddress(111);
        DatagramPacket mockDgPkt = new DatagramPacket(pkt.toByteArray(), 1, addr);
        UtpTimestampedPacketDTO toReturn = new UtpTimestampedPacketDTO(mockDgPkt, pkt, 1L, 0);

        return toReturn;
    }


    public static UtpTimestampedPacketDTO createPacket(int sequenceNumber) throws SocketException {
        return createPacket(sequenceNumber, 1);
    }

    public static UtpTimestampedPacketDTO createSelAckPacket(int i, byte[] selAck) throws SocketException {
        UtpTimestampedPacketDTO ack = PacketTestUtil.createPacket(2);
        ack.utpPacket().setAckNumber((short) (i & 0xFFFF));

        SelectiveAckHeaderExtension selAckExtension = new SelectiveAckHeaderExtension();
        selAckExtension.setBitMask(selAck);
        selAckExtension.setNextExtension((byte) 0);
        ack.utpPacket().setFirstExtension(UtpPacketUtils.SELECTIVE_ACK);

        SelectiveAckHeaderExtension[] extensions = { selAckExtension };
        ack.utpPacket().setExtensions(extensions);

        return ack;

    }
}
