/*
 * Copyright (c) 2022 Leandro José Britto de Oliveira
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
package com.github.ljbo82.jserial.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class BinaryConnection extends Connection.Wrapper {
    // region Static scope
    private static final int MESSAGE_MAX_LEN = 255;

    private static final byte[] ACK = new byte[] { 1 };
    private static final byte[] NAK = new byte[] { 0 };

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static class Message {
        private static Message parse(byte[] rawData) {
            return new Message(rawData[0], Arrays.copyOfRange(rawData, 1, rawData.length));
        }

        private final byte id;
        private final byte[] data;

        public Message(byte id, byte...data) {
            this.id = id;
            this.data = data;
        }

        public Message(Message wrapped) {
            this.id = wrapped.id;
            this.data = wrapped.data;
        }

        public final byte getId() {
            return id;
        }

        protected final byte[] getData() {
            return data;
        }

        private byte[] getRawData() {
            byte[] data = getData();
            byte[] rawData = new byte[data.length + 1];
            rawData[0] = id;
            for (int i = 0; i < data.length; i++) {
                rawData[i + 1] = data[i];
            }
            return rawData;
        }

        @Override
        public String toString() {
            return String.format("id: %02x, data: %s", id & 0xff, toHex(getData()));
        }
    }

    public static class RejectedException extends IOException {}
    // endregion

    public BinaryConnection(Connection wrapped) {
        super(wrapped);
    }

    // region Send methods
    private void send(boolean checkAck, byte[] rawData, int off, int len) throws RejectedException, IOException {
        if (rawData == null)
            throw new NullPointerException("Null data");

        if (len > MESSAGE_MAX_LEN)
            throw new IllegalArgumentException("Data length violates protocol limits");

        byte[] buffer = new byte[MESSAGE_MAX_LEN + 1];

        // Header...
        buffer[0] = (byte)len;

        // Payload
        int i;
        for (i = 0 ; i < len; i++) {
            buffer[i + 1] = rawData[i + off];
        }

        OutputStream os = getOutputStream();
        os.write(buffer, 0, i + 1);
        os.flush();

        if (checkAck) {
            byte[] ack = readRaw();

            if (!Arrays.equals(ack, ACK)) {
                throw new RejectedException();
            }
        }
    }

    public void send(byte[] rawData, int off, int len) throws RejectedException, IOException {
        send(true, rawData, off, len);
    }

    public void send(byte[] rawData) throws RejectedException, IOException {
        send(rawData, 0, rawData.length);
    }

    public void send(Message message) throws RejectedException, IOException {
        send(message.getRawData());
    }

    public void sendAck(boolean accepted) throws IOException {
        byte[] data = accepted ? ACK : NAK;
        send(false, data, 0, data.length);
    }
    // endregion

    //region Read methods
    public byte[] readRaw() throws IOException {
        InputStream is = getInputStream();
        byte[] messageBuffer = new byte[MESSAGE_MAX_LEN];

        int messageLength = is.read();
        if (messageLength <= 0) // EOS or no data with no read timeout
            return null;

        messageLength = messageLength & 0xff;

        int totalRead = 0;
        int read;
        while (totalRead < messageLength) {
            read = is.read(messageBuffer, totalRead, messageLength - totalRead);

            if (read <= 0) { // EOS or no data with no read timeout
                return null;
            }

            totalRead += read;
        }

        return Arrays.copyOfRange(messageBuffer, 0, totalRead);
    }

    public Message readMessage() throws IOException {
        return Message.parse(readRaw());

    }
    // endregion
}
