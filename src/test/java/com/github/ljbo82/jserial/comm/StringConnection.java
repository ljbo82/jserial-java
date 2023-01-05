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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StringConnection extends Connection.Wrapper {
    // region Static scope
    private static final int    MESSAGE_MAX_LEN           = 1024;
    private static final char   MESSAGE_DELIMITER         = '\n';
    private static final char   MESSAGE_PARAM_DELIMITER   = ';';
    private static final String ACK                       = "ACK";
    private static final String NAK                       = "NAK";

    private static void append(ByteArrayOutputStream bos, char c) {
        bos.write(((byte)c) & 0xff);
    }

    private static void append(ByteArrayOutputStream bos, byte b) {
        bos.write(b & 0xff);
    }

    private static String escape(String msg, boolean escapeParamDelimiter) {
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);

        if (data.length > MESSAGE_MAX_LEN)
            throw new IllegalArgumentException("Message length violates protocol limits");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (byte b : data) {
            switch (b) {
                case MESSAGE_DELIMITER:
                    append(buffer, '\\');
                    append(buffer, 'n');
                    break;

                case MESSAGE_PARAM_DELIMITER:
                    if (escapeParamDelimiter) {
                        append(buffer, '\\');
                        append(buffer, ';');
                    } else {
                        append(buffer, b);
                    }
                    break;

                case '\\':
                    append(buffer, '\\');
                    append(buffer, '\\');
                    break;


                default:
                    append(buffer, b);
                    break;
            }
        }

        return new String(buffer.toByteArray(), 0, buffer.size(), StandardCharsets.UTF_8);
    }

    private static String unescape(String msg, boolean unescapeParamDelimiter) throws IOException {
        if (msg.length() > 2 * MESSAGE_MAX_LEN)
            throw new IOException("Message length violates protocol limits");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean escaping = false;

        for (byte b : msg.getBytes(StandardCharsets.UTF_8)) {
            if (!escaping && (b == '\\')) {
                escaping = true;
            } else { // escaping || c != '\\'
                if (!escaping) {
                    append(buffer, b);
                } else { // escaping
                    escaping = false;
                    switch (b) {
                        case 'n':
                            append(buffer, '\n');
                            break;

                        case '\\':
                        case ';':
                            if (unescapeParamDelimiter) {
                                append(buffer, b);
                            } else {
                                throw new IOException(String.format("Unknown escape sequence at byte %d", buffer.size()));
                            }
                            break;
                        default:
                            throw new IOException(String.format("Unknown escape sequence at byte %d", buffer.size()));
                    }
                }
            }
        }

        if (escaping)
            throw new IOException(String.format("Invalid escape at byte %d", buffer.size()));

        return new String(buffer.toByteArray(), 0, buffer.size(), StandardCharsets.UTF_8);
    }

    public static class Message {
        private final String id;
        private final String[] params;

        private static Message parse(String rawMsg) throws IOException {
            String regex = "(?<!\\\\)" + MESSAGE_PARAM_DELIMITER;

            String[] tokens = rawMsg.split(regex);
            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = unescape(tokens[i], true);
            }
            String id = tokens[0];
            String[] params = Arrays.copyOfRange(tokens, 1, tokens.length);
            return new Message(id, params);
        }

        public Message(String id, String...params) {
            this.id = id;
            this.params = params;
        }

        public Message(Message wrapped) {
            this.id = wrapped.id;
            this.params = wrapped.params;
        }

        public final String getId() {
            return id;
        }

        protected final String[] getParams() {
            return params;
        }

        private String toRaw() {
            StringBuilder sb = new StringBuilder(id == null ? "" : escape(id, true));
            for (String param : params) {
                sb.append(MESSAGE_PARAM_DELIMITER).append(param == null ? "" : escape(param, true));
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return toRaw();
        }
    }

    public static class RejectedException extends IOException {}
    // endregion

    public StringConnection(Connection wrapped) {
        super(wrapped);
    }

    // region Send methods
    private void send(boolean checkAck, String rawMessage) throws RejectedException, IOException {
        OutputStream os = getOutputStream();

        rawMessage = String.format("%s%c", rawMessage, MESSAGE_DELIMITER);

        os.write(rawMessage.getBytes(StandardCharsets.UTF_8));
        os.flush();

        if (checkAck) {
            String ack = readRaw();
            switch (ack) {
                case NAK:
                    throw new RejectedException();

                case ACK:
                    return;

                default:
                    throw new IOException("Unknown response");
            }
        }
    }

    public void send(String rawMessage) throws RejectedException, IOException {
        send(true, escape(rawMessage, false));
    }

    public void send(Message message) throws RejectedException, IOException {
        send(true, message.toRaw());
    }

    public void sendAck(boolean accepted) throws IOException {
        send(false, accepted ? ACK : NAK);
    }
    // endregion

    // region Read methods
    public String readRaw() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        InputStream is = getInputStream();

        int b;
        while (buffer.size() < 2 * MESSAGE_MAX_LEN) {
            if ((b = is.read()) <= 0) // EOS or no data with no read timeout
                return null;

            if (b == MESSAGE_DELIMITER) {
                return unescape(new String(buffer.toByteArray(), 0, buffer.size(), StandardCharsets.UTF_8), false);
            }

            buffer.write(b);
        }

        // Message delimiter not found (message is too long)
        purge();
        throw new IOException("Message length violates protocol limits");
    }

    public Message readMessage() throws IOException {
        return Message.parse(readRaw());
    }
    // endregion
}
