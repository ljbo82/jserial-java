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

import com.github.ljbo82.jserial.SerialPort;
import com.github.ljbo82.jserial.SerialPort.DataBits;
import com.github.ljbo82.jserial.SerialPort.Parity;
import com.github.ljbo82.jserial.SerialPort.StopBits;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPortConnection extends Connection {
    private static final int      DEFAULT_BAUD                = 9600;
    private static final long     DEFAULT_READ_TIMEOUT_MILLIS = 3000;
    private static final DataBits DEFAULT_DATA_BITS           = DataBits.DATA_BITS_8;
    private static final Parity   DEFAULT_PARITY              = Parity.NONE;
    private static final StopBits DEFAULT_STOP_BITS           = StopBits.STOP_BITS_1;

    private final SerialPort serialPort;

    public SerialPortConnection(String portName) throws IOException {
        serialPort = new SerialPort(portName);
        serialPort.setReadTimeout(DEFAULT_READ_TIMEOUT_MILLIS);
        serialPort.config(DEFAULT_BAUD, DEFAULT_DATA_BITS, DEFAULT_PARITY, DEFAULT_STOP_BITS);
        purge();
    }

    public void config(int baud, DataBits dataBits, Parity parity, StopBits stopBits) throws IOException {
        if (dataBits == null || parity == null || stopBits == null)
            throw new NullPointerException();

        if (baud != serialPort.getBaud() || dataBits != serialPort.getDataBits() || parity != serialPort.getParity() || stopBits != serialPort.getStopBits()) {
            serialPort.config(baud, dataBits, parity, stopBits);
            purge();
        }
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
            //noinspection StatementWithEmptyBody
            while (getInputStream().read() > 0) {}
        } catch (SerialPort.TimeoutException e) {
            // ignore error
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
}
