package com.github.ljbo82.jserial.test;

import com.github.ljbo82.jserial.SerialPort.DataBits;
import com.github.ljbo82.jserial.SerialPort.Parity;
import com.github.ljbo82.jserial.SerialPort.StopBits;
import com.github.ljbo82.jserial.comm.Connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class PacketConnection {
    private static final int PACKET_ACK  = 0;
    private static final int PACKET_PING = 2;
    private static final int PACKET_PROT = 3;
    private static final int PACKET_DBG  = 4;

    private static final int DATA_BITS_5 = 5 << 24;
    private static final int DATA_BITS_6 = 6 << 24;
    private static final int DATA_BITS_7 = 7 << 24;
    private static final int DATA_BITS_8 = 8 << 24;

    public static int toInt(DataBits dataBits) {
        switch (dataBits) {
        case DATA_BITS_5:
            return DATA_BITS_5;
        case DATA_BITS_6:
            return DATA_BITS_6;
        case DATA_BITS_7:
            return DATA_BITS_7;
        case DATA_BITS_8:
            return DATA_BITS_8;
        default:
            throw new UnsupportedOperationException();
        }
    }

    private static final int PARITY_NONE = 0;
    private static final int PARITY_EVEN = 1 << 16;
    private static final int PARITY_ODD  = 2 << 16;

    public static int toInt(Parity parity) {
        switch (parity) {
        case NONE:
            return PARITY_NONE;
        case EVEN:
            return PARITY_EVEN;
        case ODD:
            return PARITY_ODD;
        default:
            throw new UnsupportedOperationException();
        }
    }

    private static final int STOP_BITS_1 = 1 << 8;
    private static final int STOP_BITS_2 = 2 << 8;

    public static int toInt(StopBits stopBits) {
        switch (stopBits) {
            case STOP_BITS_1:
                return STOP_BITS_1;
            case STOP_BITS_2:
            case STOP_BITS_1_5:
                return STOP_BITS_2;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static final int CONFIG_5N1 = DATA_BITS_5 | PARITY_NONE | STOP_BITS_1;
    private static final int CONFIG_6N1 = DATA_BITS_6 | PARITY_NONE | STOP_BITS_1;
    private static final int CONFIG_7N1 = DATA_BITS_7 | PARITY_NONE | STOP_BITS_1;
    private static final int CONFIG_8N1 = DATA_BITS_8 | PARITY_NONE | STOP_BITS_1;

    private static final int CONFIG_5N2 = DATA_BITS_5 | PARITY_NONE | STOP_BITS_2;
    private static final int CONFIG_6N2 = DATA_BITS_6 | PARITY_NONE | STOP_BITS_2;
    private static final int CONFIG_7N2 = DATA_BITS_7 | PARITY_NONE | STOP_BITS_2;
    private static final int CONFIG_8N2 = DATA_BITS_8 | PARITY_NONE | STOP_BITS_2;

    private static final int CONFIG_5E1 = DATA_BITS_5 | PARITY_EVEN | STOP_BITS_1;
    private static final int CONFIG_6E1 = DATA_BITS_6 | PARITY_EVEN | STOP_BITS_1;
    private static final int CONFIG_7E1 = DATA_BITS_7 | PARITY_EVEN | STOP_BITS_1;
    private static final int CONFIG_8E1 = DATA_BITS_8 | PARITY_EVEN | STOP_BITS_1;

    private static final int CONFIG_5E2 = DATA_BITS_5 | PARITY_EVEN | STOP_BITS_2;
    private static final int CONFIG_6E2 = DATA_BITS_6 | PARITY_EVEN | STOP_BITS_2;
    private static final int CONFIG_7E2 = DATA_BITS_7 | PARITY_EVEN | STOP_BITS_2;
    private static final int CONFIG_8E2 = DATA_BITS_8 | PARITY_EVEN | STOP_BITS_2;

    private static final int CONFIG_5O1 = DATA_BITS_5 | PARITY_ODD | STOP_BITS_1;
    private static final int CONFIG_6O1 = DATA_BITS_6 | PARITY_ODD | STOP_BITS_1;
    private static final int CONFIG_7O1 = DATA_BITS_7 | PARITY_ODD | STOP_BITS_1;
    private static final int CONFIG_8O1 = DATA_BITS_8 | PARITY_ODD | STOP_BITS_1;

    private static final int CONFIG_5O2 = DATA_BITS_5 | PARITY_ODD | STOP_BITS_2;
    private static final int CONFIG_6O2 = DATA_BITS_6 | PARITY_ODD | STOP_BITS_2;
    private static final int CONFIG_7O2 = DATA_BITS_7 | PARITY_ODD | STOP_BITS_2;
    private static final int CONFIG_8O2 = DATA_BITS_8 | PARITY_ODD | STOP_BITS_2;

    private final com.github.ljbo82.jserial.comm.PacketConnection connection;

    private static byte toByte(DataBits dataBits, Parity parity, StopBits stopBits) {
        switch (toInt(dataBits) | toInt(parity) | toInt(stopBits)) {
        case CONFIG_5N1: return 0x00;
        case CONFIG_6N1: return 0x02;
        case CONFIG_7N1: return 0x04;
        case CONFIG_8N1: return 0x06;

        case CONFIG_5N2: return 0x08;
        case CONFIG_6N2: return 0x0a;
        case CONFIG_7N2: return 0x0c;
        case CONFIG_8N2: return 0x0e;

        case CONFIG_5E1: return 0x20;
        case CONFIG_6E1: return 0x22;
        case CONFIG_7E1: return 0x24;
        case CONFIG_8E1: return 0x26;

        case CONFIG_5E2: return 0x28;
        case CONFIG_6E2: return 0x2a;
        case CONFIG_7E2: return 0x2c;
        case CONFIG_8E2: return 0x2e;

        case CONFIG_5O1: return 0x30;
        case CONFIG_6O1: return 0x32;
        case CONFIG_7O1: return 0x34;
        case CONFIG_8O1: return 0x36;

        case CONFIG_5O2: return 0x38;
        case CONFIG_6O2: return 0x3a;
        case CONFIG_7O2: return 0x3c;
        case CONFIG_8O2: return 0x3e;

        default:
            throw new UnsupportedOperationException();
        }
    }

    private static byte toByte(Mode mode) {
        switch (mode) {
        case MESSAGE:
            return 0;

        case PACKET:
            return 1;

        default:
            throw new UnsupportedOperationException();
        }
    }

    public PacketConnection(Connection wrapped) {
        connection = new com.github.ljbo82.jserial.comm.PacketConnection(wrapped);
    }

    private byte[] readPacket() throws IOException {
        while(true) {
            byte[] packet = connection.read();
            if (packet != null && packet.length > 0 && packet[0] != PACKET_DBG) {
                return packet;
            }
        }
    }

    public void ping(String msg) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(PACKET_PING);
        bos.write(msg.getBytes(StandardCharsets.UTF_8));

        connection.write(bos.toByteArray());
        byte[] rsp = readPacket();

        if (rsp.length == 0)
            throw new IOException("No response");

        if (rsp[0] != PACKET_ACK)
            throw new IOException("Packet was rejected");

        rsp = readPacket();
        if (rsp.length == 0)
            throw new IOException("No response");

        String strRsp = new String(rsp, 0, rsp.length, StandardCharsets.UTF_8);
        if (!strRsp.equals(msg))
            throw new IOException("Invalid response");
    }

    public void protocol(int baud, DataBits dataBits, Parity parity, StopBits stopBits, Mode mode) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        bos.write(PACKET_PROT);
        bos.write(ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(baud)
            .put(toByte(dataBits, parity, stopBits))
            .put(toByte(mode))
            .array()
        );

        connection.write(bos.toByteArray());
        byte[] rsp = readPacket();

        if (rsp.length == 0)
            throw new IOException("No response");

        if (rsp[0] != PACKET_ACK)
            throw new IOException("Packet was rejected");
    }
}
