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
package com.github.ljbo82.jserial;

import java.io.IOException;
import java.io.InputStream;

class SerialPortInputStream extends InputStream {
    private static final byte[] SKIP_BUFFER = new byte[1204];

    private final SerialPort serialPort;

    public SerialPortInputStream(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    @Override
    public void close() throws IOException {
        serialPort.close();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            serialPort.semaphore.acquire();

            // SerialPort.close() cannot be called simultaneously if this point was reached

            if (serialPort.nativeSerialPort == SerialPort.CLOSED_NATIVE_PORT) {
                // Port is closed (EOS)
                serialPort.semaphore.release();
                return -1;
            }

            int mRead = NativeSerialPort.read(serialPort.nativeSerialPort, b, off, len);
            serialPort.semaphore.release();

            if (mRead < 0) {
                NativeSerialPort.throwNativeError();
            }

            return mRead;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read() throws IOException {
        byte[] singleByteBuffer = new byte[1];

        int mRead = read(singleByteBuffer);

        if (mRead < 0)
            return mRead;

        return singleByteBuffer[0] & 0xff;
    }

    @Override
    public long skip(long n) throws IOException {
        long remaining = n;
        int read;

        while (remaining > 0) {
            read = read(SKIP_BUFFER);

            if (read <= 0) {
                break;
            }

            remaining -= read;
        }

        return n - remaining;

    }
}
