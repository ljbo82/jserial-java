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
package com.github.ljbo82.jserial.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class PacketConnection extends Connection.Wrapper {
    // region Static scope
    private static final int MESSAGE_MAX_LEN = 255;
    // endregion

    public PacketConnection(Connection wrapped) {
        super(wrapped);
    }

    public byte[] read() throws IOException {
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

    public void write(byte[] data, int off, int len) throws IOException {
        if (data == null)
            throw new NullPointerException("Null data");

        if (len > MESSAGE_MAX_LEN)
            throw new IllegalArgumentException("Data length violates protocol limits");

        byte[] buffer = new byte[MESSAGE_MAX_LEN + 1];

        // Header
        buffer[0] = (byte)len;

        // Payload
        int i;
        for (i = 0 ; i < len; i++) {
            buffer[i + 1] = data[i + off];
        }

        OutputStream os = getOutputStream();
        os.write(buffer, 0, i + 1);
        os.flush();
    }

    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }
}
