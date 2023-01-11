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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LineConnection extends Connection.Wrapper {
    // region Static scope
    private static final int  MESSAGE_MAX_LEN   = 1024;
    private static final char MESSAGE_DELIMITER = '\r';
    // endregion

    public LineConnection(Connection wrapped) {
        super(wrapped);
    }

    public String read() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        InputStream is = getInputStream();

        int b;
        while (buffer.size() < 2 * MESSAGE_MAX_LEN) {
            if ((b = is.read()) <= 0) // EOS or no data with no read timeout
                return null;

            if (b == MESSAGE_DELIMITER) {
                return new String(buffer.toByteArray(), 0, buffer.size(), StandardCharsets.UTF_8);
            }

            buffer.write(b);
        }

        // Message delimiter not found (message is too long)
        purge();
        throw new IOException("Message length violates protocol limits");
    }

    public void write(String msg) throws IOException {
        byte[] data;

        if (msg.endsWith("" + MESSAGE_DELIMITER)) {
            data = msg.getBytes(StandardCharsets.UTF_8);
        } else {
            data = String.format("%s%c", msg, MESSAGE_DELIMITER).getBytes(StandardCharsets.UTF_8);
        }

        OutputStream os = getOutputStream();

        os.write(data);
        os.flush();
    }
}
