package com.github.ljbo82.jserial.test;

import com.github.ljbo82.jserial.SerialPort.DataBits;
import com.github.ljbo82.jserial.SerialPort.Parity;
import com.github.ljbo82.jserial.SerialPort.StopBits;
import com.github.ljbo82.jserial.comm.Connection;
import com.github.ljbo82.jserial.comm.LineConnection;

import java.io.IOException;

public class MessageConnection {
    private static final String MSG_PING     = "PING";
    private static final String MSG_PROT     = "PROT";
    private static final String MSG_ACK      = "ACK";
    private static final String DEBUG_PREFIX = "\033[90m";

    private static String toString(DataBits dataBits, Parity parity, StopBits stopBits) {
        StringBuilder sb = new StringBuilder();
        switch (dataBits) {
        case DATA_BITS_5:
            sb.append("5");
            break;
        case DATA_BITS_6:
            sb.append("6");
            break;
        case DATA_BITS_7:
            sb.append("7");
            break;
        case DATA_BITS_8:
            sb.append("8");
            break;

        default:
            throw new UnsupportedOperationException();
        }

        switch (parity) {
        case EVEN:
            sb.append("E");
            break;

        case ODD:
            sb.append("O");
            break;

        case NONE:
            sb.append("N");
            break;

        default:
            throw new UnsupportedOperationException();
        }

        switch (stopBits) {
        case STOP_BITS_1:
            sb.append("1");
            break;
        case STOP_BITS_1_5:
            sb.append("1.5");
            break;

        case STOP_BITS_2:
            sb.append("2");
            break;

        default:
            throw new UnsupportedOperationException();
        }
        return sb.toString();
    }
    
    private final LineConnection connection;

    public MessageConnection(Connection wrapped) {
        connection = new LineConnection(wrapped);
    }

    private String readMessage() throws IOException {
        while(true) {
            String msg = connection.read();
            if (msg != null && !msg.startsWith(DEBUG_PREFIX)) {
                return msg;
            }
        }
    }

    public void ping(String msg) throws IOException {
        if (msg.contains("\n"))
            throw new IllegalArgumentException("Invalid msg");

        connection.write(String.format("%s;%s", MSG_PING, msg));
        String rsp = readMessage();
        if (!rsp.equals(MSG_ACK))
            throw new IOException("Message was rejected");

        rsp = readMessage();
        if (!rsp.equals(msg))
            throw new IOException("Invalid response");
    }

    public void protocol(int baud, DataBits dataBits, Parity parity, StopBits stopBits, Mode mode) throws IOException {
        if (dataBits == null || parity == null || stopBits == null || mode == null)
            throw new NullPointerException();

        StringBuilder sb = new StringBuilder(MSG_PROT).append(";")
            .append(baud).append(";")
            .append(toString(dataBits, parity, stopBits)).append(";");

        switch (mode) {
        case MESSAGE:
            sb.append("0");
            break;
        case PACKET:
            sb.append("1");
            break;

        default:
            throw new UnsupportedOperationException();
        }

        connection.write(sb.toString());
        String rsp = readMessage();
        if (!rsp.equals(MSG_ACK))
            throw new IOException("Message was rejected");
    }
}
