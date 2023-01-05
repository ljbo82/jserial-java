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

import com.github.ljbo82.jserial.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPortConnection extends Connection {
    private static final int                 DEFAULT_BAUD                = 9600;
    private static final long                DEFAULT_READ_TIMEOUT_MILLIS = 3000;
    private static final SerialPort.DataBits DEFAULT_DATA_BITS           = SerialPort.DataBits.DATA_BITS_8;
    private static final SerialPort.Parity   DEFAULT_PARITY              = SerialPort.Parity.EVEN;
    private static final SerialPort.StopBits DEFAULT_STOP_BITS           = SerialPort.StopBits.STOP_BITS_1;

    private final SerialPort serialPort;

    public SerialPortConnection(String portName) throws IOException {
        serialPort = new SerialPort(portName);
        serialPort.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLIS);
        serialPort.config(DEFAULT_BAUD, DEFAULT_DATA_BITS, DEFAULT_PARITY, DEFAULT_STOP_BITS);
        purge();
    }

    @Override
    public boolean isOpen() {
        return serialPort.isOpen();
    }

    @Override
    protected InputStream getInputStream() {
        return serialPort.getInputStream();
    }

    @Override
    protected OutputStream getOutputStream() {
        return serialPort.getOutputStream();
    }

    @Override
    public void purge() throws IOException {
        try {
            while (getInputStream().read() > 0) {}
        } catch (SerialPort.TimeoutException e) {
            // ignore error
        } finally {
            serialPort.purgeRX();
            serialPort.purgeTX();
        }
    }

    @Override
    public void close() throws IOException {
        serialPort.close();
    }

    @Override
    public String toString() {
        return serialPort.toString();
    }

    public final SerialPort getSerialPort() {
        return serialPort;
    }
}
