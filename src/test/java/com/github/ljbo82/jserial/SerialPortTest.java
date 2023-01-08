/*
 * Copyright (c) 2023 Leandro José Britto de Oliveira
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
package com.github.ljbo82.jserial;

import com.github.ljbo82.jserial.comm.BinaryConnection;
import com.github.ljbo82.jserial.comm.Connection;
import com.github.ljbo82.jserial.comm.SerialPortConnection;
import com.github.ljbo82.jserial.comm.StringConnection;
import org.junit.Assert;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class SerialPortTest {
    public static class ProtocolException extends IOException {
        public ProtocolException(String msg, Object...args) {
            super(args.length == 0 ? msg : String.format(msg, args));
        }
    }

    // region StringConnection messages
    public static class StringConnection extends com.github.ljbo82.jserial.comm.StringConnection {
        private static final char DEBUG_MESSAGE_PREFIX = '\033';

        public StringConnection(Connection wrapped) {
            super(wrapped);
        }

        @Override
        public String readRaw() throws IOException {
            while(true) {
                String rawMessage = super.readRaw();
                if (rawMessage.charAt(0) != DEBUG_MESSAGE_PREFIX) {
                    return rawMessage;
                }
            }
        }
    }

    public static class PingStringMessage extends StringConnection.Message {
        private static final String ID = "PING";

        public PingStringMessage(String msg) {
            super(ID, msg);
        }

        public PingStringMessage(StringConnection.Message wrapped) throws ProtocolException {
            super(wrapped);

            if (!getId().equals(ID))
                throw new ProtocolException("Invalid ID (expected: %s, given: %s)", ID, wrapped.getId());

            if (getParams().length != -1)
                throw new ProtocolException("Invalid number of parameters (expected: %d, given: %d)", 1, getParams().length);
        }

        public final String getMessage() {
            return getParams()[0];
        }
    }

    public static class SerialConfigStringMessage extends StringConnection.Message {
        private static final String ID = "SERIALCFG";

        private static String[] getParams(int baud, SerialPort.DataBits dataBits, SerialPort.Parity parity, SerialPort.StopBits stopBits) {
            String[] params = new String[2];
            params[0] = Integer.toString(baud);

            int mDataBits;
            switch (dataBits) {
                case DATA_BITS_5:
                    mDataBits = 5;
                    break;

                case DATA_BITS_6:
                    mDataBits = 6;
                    break;

                case DATA_BITS_7:
                    mDataBits = 7;
                    break;

                case DATA_BITS_8:
                    mDataBits = 8;
                    break;

                default:
                    throw new UnsupportedOperationException();
            }

            char mParity;
            switch (parity) {
                case NONE:
                    mParity = 'N';
                    break;

                case EVEN:
                    mParity = 'E';
                    break;

                case ODD:
                    mParity = 'O';
                    break;

                default:
                    throw new UnsupportedOperationException();
            }

            String mStopBits;
            switch (stopBits) {
                case STOP_BITS_1:
                    mStopBits = "1";
                    break;

                case STOP_BITS_2:
                    mStopBits = "2";
                    break;

                case STOP_BITS_1_5:
                    mStopBits = "1.5";
                    break;

                default:
                    throw new UnsupportedOperationException();
            }

            params[1] = String.format("%d%c%s", mDataBits, mParity, mStopBits);
            return params;
        }

        public SerialConfigStringMessage(int baud, SerialPort.DataBits dataBits, SerialPort.Parity parity, SerialPort.StopBits stopBits) {
            super(ID, getParams(baud, dataBits, parity, stopBits));
        }
    }

    public static class ProtocolSwitchStringMessage extends StringConnection.Message {
        private static final String ID = "PROTSW";

        public ProtocolSwitchStringMessage() {
            super(ID);
        }
    }
    // endregion

    // region BinaryConnection messages
    public static class BinaryConnection extends com.github.ljbo82.jserial.comm.BinaryConnection {
        private static final byte DEBUG_MESSAGE_ID = 0;

        public BinaryConnection(Connection wrapped) {
            super(wrapped);
        }

        @Override
        public byte[] readRaw() throws IOException {
            while(true) {
                byte[] rawMessage = super.readRaw();
                if (rawMessage[0] != DEBUG_MESSAGE_ID) {
                    return rawMessage;
                }
            }
        }
    }

    public static class PingBinaryMessage extends BinaryConnection.Message {
        private static final byte ID = 0x1;
        private final String msg;

        public PingBinaryMessage(String msg) {
            super(ID, msg == null ? "".getBytes(StandardCharsets.UTF_8) : msg.getBytes(StandardCharsets.UTF_8));
            this.msg = msg;
        }

        public PingBinaryMessage(BinaryConnection.Message wrapped) throws ProtocolException {
            super(wrapped);
            if (getId() != ID)
                throw new ProtocolException("Invalid ID (expected: %s, given: %s)", ID, wrapped.getId());

            msg = new String(getData(), StandardCharsets.UTF_8);
        }

        public final String getMessage() {
            return msg;
        }

        @Override
        public String toString() {
            return String.format("[PING] %s", msg);
        }
    }

    public static class SerialConfigBinaryMessage extends BinaryConnection.Message {
        private static final byte ID = 0x2;

        private final int baud;
        private final SerialPort.DataBits dataBits;
        private final SerialPort.Parity parity;
        private final SerialPort.StopBits stopBits;

        private static byte[] getParams(int baud, SerialPort.DataBits dataBits, SerialPort.Parity parity, SerialPort.StopBits stopBits) {
            ByteBuffer buffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);

            byte mDataBits;
            switch (dataBits) {
                case DATA_BITS_5:
                    mDataBits = 5;
                    break;

                case DATA_BITS_6:
                    mDataBits = 6;
                    break;

                case DATA_BITS_7:
                    mDataBits = 7;
                    break;

                case DATA_BITS_8:
                    mDataBits = 8;
                    break;

                default:
                    throw new UnsupportedOperationException();
            }

            byte mParity;
            switch (parity) {
                case NONE:
                    mParity = 0;
                    break;

                case EVEN:
                    mParity = 2;
                    break;

                case ODD:
                    mParity = 1;
                    break;

                default:
                    throw new UnsupportedOperationException();
            }

            byte mStopBits;
            switch (stopBits) {
                case STOP_BITS_1:
                    mStopBits = 1;
                    break;

                case STOP_BITS_2:
                    mStopBits = 2;
                    break;

                case STOP_BITS_1_5:
                    mStopBits = 3;
                    break;

                default:
                    throw new UnsupportedOperationException();
            }

            buffer.putInt(baud).put(mDataBits).put(mParity).putInt(mStopBits);

            return buffer.array();
        }

        public SerialConfigBinaryMessage(int baud, SerialPort.DataBits dataBits, SerialPort.Parity parity, SerialPort.StopBits stopBits) {
            super(ID, getParams(baud, dataBits, parity, stopBits));
            this.baud = baud;
            this.dataBits = dataBits;
            this.parity = parity;
            this.stopBits = stopBits;
        }

        @Override
        public String toString() {
            String[] params = SerialConfigStringMessage.getParams(baud, dataBits, parity, stopBits);
            return String.format("[SERIALCFG] %s %s", params[0], params[1]);
        }
    }

    public static class ProtocolSwitchBinaryMessage extends BinaryConnection.Message {
        private static final byte ID = 0x3;

        public ProtocolSwitchBinaryMessage() {
            super(ID);
        }
    }
    // endregion

    private static void doStringConnectionTests(StringConnection connection, SerialPortConnection serialConnection) throws IOException {
        connection.send(new PingStringMessage("Hello"));
        Assert.assertEquals(new PingStringMessage(connection.readMessage()).getMessage(), "Hello");

        connection.send(new SerialConfigStringMessage(2400, SerialPort.DataBits.DATA_BITS_7, SerialPort.Parity.EVEN, SerialPort.StopBits.STOP_BITS_2));
        serialConnection.getSerialPort().config(2400, SerialPort.DataBits.DATA_BITS_7, SerialPort.Parity.EVEN, SerialPort.StopBits.STOP_BITS_2);

        connection.send(new PingStringMessage("world!"));
        Assert.assertEquals(new PingStringMessage(connection.readMessage()).getMessage(), "world!");
    }

    private static void doBinaryConnectionTests(BinaryConnection connection, SerialPortConnection serialConnection) throws IOException {
        connection.send(new PingBinaryMessage("Hello"));
        Assert.assertEquals(new PingBinaryMessage(connection.readMessage()).getMessage(), "Hello");

        connection.send(new SerialConfigBinaryMessage(2400, SerialPort.DataBits.DATA_BITS_7, SerialPort.Parity.EVEN, SerialPort.StopBits.STOP_BITS_2));
        serialConnection.getSerialPort().config(2400, SerialPort.DataBits.DATA_BITS_7, SerialPort.Parity.EVEN, SerialPort.StopBits.STOP_BITS_2);

        connection.send(new PingBinaryMessage("world!"));
        Assert.assertEquals(new PingBinaryMessage(connection.readMessage()).getMessage(), "world!");
    }

    public static void main(String[] args) throws IOException {
        String[] ports = SerialPort.getPortNames();
        if (ports.length == 0)
            throw new RuntimeException("No ports detected");

        String portName = ports[ports.length == 1 ? 0 : TestUtils.getOptionIndex("Choose a port: ", ports)];
        System.out.printf("Opening port %s... ", portName);
        System.out.flush();
        try (SerialPortConnection serialConnection = new SerialPortConnection(portName)) {
            System.out.println("DONE!\n");

            int baud = serialConnection.getSerialPort().getBaud();
            SerialPort.DataBits dataBits = serialConnection.getSerialPort().getDataBits();
            SerialPort.Parity parity = serialConnection.getSerialPort().getParity();
            SerialPort.StopBits stopBits = serialConnection.getSerialPort().getStopBits();

            StringConnection stringConnection = new StringConnection(serialConnection);
            doStringConnectionTests(stringConnection, serialConnection);
            stringConnection.send(new ProtocolSwitchStringMessage());
            serialConnection.getSerialPort().config(baud, dataBits, parity, stopBits);

            BinaryConnection binaryConnection = new BinaryConnection(serialConnection);
            doBinaryConnectionTests(binaryConnection, serialConnection);
            binaryConnection.send(new ProtocolSwitchBinaryMessage());
            serialConnection.getSerialPort().config(baud, dataBits, parity, stopBits);
        }
    }
}
