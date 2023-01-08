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

import java.io.IOException;
import java.io.OutputStream;

class SerialPortOutputStream extends OutputStream {
    private final SerialPort serialPort;

    SerialPortOutputStream(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    @Override
    public void close() throws IOException {
        serialPort.close();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            serialPort.semaphore.acquire();

            // SerialPort.close() cannot be called simultaneously if this point was reached

            if (serialPort.nativeSerialPort == SerialPort.CLOSED_NATIVE_PORT) {
                // Port is closed
                serialPort.semaphore.release();
                throw new IOException("Port is closed");
            }

            boolean success = NativeSerialPort.write(serialPort.nativeSerialPort, b, off, len);
            serialPort.semaphore.release();

            if (!success) {
                NativeSerialPort.throwNativeError();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(int i) throws IOException {
        byte[] singleByteBuffer = new byte[1];
        singleByteBuffer[0] = (byte)i;
        write(singleByteBuffer);
    }

    @Override
    public void flush() throws IOException {
        try {
            serialPort.semaphore.acquire();

            // SerialPort.close() cannot be called simultaneously if this point was reached

            if (serialPort.nativeSerialPort == SerialPort.CLOSED_NATIVE_PORT) {
                // Port is closed
                serialPort.semaphore.release();
                throw new IOException("Port is closed");
            }

            boolean success = NativeSerialPort.flush(serialPort.nativeSerialPort);
            serialPort.semaphore.release();

            if (!success) {
                NativeSerialPort.throwNativeError();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
