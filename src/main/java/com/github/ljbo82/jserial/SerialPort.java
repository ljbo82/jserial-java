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
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class SerialPort implements AutoCloseable {
    private static final int SEMAPHORE_MAX_PERMITS =  2; // Read & Write
    /*pp*/  static final int CLOSED_NATIVE_PORT    = -1;
    private static boolean inited;

    public enum DataBits {
        DATA_BITS_5(5),
        DATA_BITS_6(6),
        DATA_BITS_7(7),
        DATA_BITS_8(8);

        private static final Map<Integer, DataBits> MAP;

        static {
            HashMap<Integer, DataBits> map = new HashMap<>();
            for (DataBits dataBits : DataBits.values()) {
                map.put(dataBits.nativeCode, dataBits);
            }
            MAP = Collections.unmodifiableMap(map);
        }

        static DataBits fromNativeCode(int nativeCode) {
            DataBits dataBits = MAP.get(nativeCode);

            if (dataBits == null)
                throw new UnsupportedOperationException(String.format("Missing support for nativeCode == %d", nativeCode));

            return dataBits;
        }

        private final int nativeCode;

        DataBits(int nativeCode) {
            this.nativeCode = nativeCode;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    public enum Parity {
        NONE(0, "N"),
        EVEN(1, "E"),
        ODD(2, "O");

        private static final Map<Integer, Parity> MAP;

        static {
            HashMap<Integer, Parity> map = new HashMap<>();
            for (Parity parity : Parity.values()) {
                map.put(parity.nativeCode, parity);
            }
            MAP = Collections.unmodifiableMap(map);
        }

        static Parity fromNativeCode(int nativeCode) {
            Parity parity = MAP.get(nativeCode);

            if (parity == null)
                throw new UnsupportedOperationException(String.format("Missing support for nativeCode == %d", nativeCode));

            return parity;
        }

        private final int nativeCode;
        private final String protocolStrToken;

        Parity(int nativeCode, String protocolStrToken) {
            this.nativeCode = nativeCode;
            this.protocolStrToken = protocolStrToken;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    public enum StopBits {
        STOP_BITS_1(1,   "1"),
        STOP_BITS_2(2,   "2"),
        STOP_BITS_1_5(3, "1.5");

        private static final Map<Integer, StopBits> MAP;

        static {
            HashMap<Integer, StopBits> map = new HashMap<>();
            for (StopBits stopBits : StopBits.values()) {
                map.put(stopBits.nativeCode, stopBits);
            }
            MAP = Collections.unmodifiableMap(map);
        }

        static StopBits fromNativeCode(int nativeCode) {
            StopBits stopBits = MAP.get(nativeCode);

            if (stopBits == null)
                throw new UnsupportedOperationException(String.format("Missing support for nativeCode == %d", nativeCode));

            return stopBits;
        }

        private final int nativeCode;
        private final String protocolStrToken;

        StopBits(int nativeCode, String protocolStrToken) {
            this.nativeCode = nativeCode;
            this.protocolStrToken = protocolStrToken;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    enum PurgeType {
        RX(0),
        TX(1),
        RX_TX(2);

        private final int nativeCode;

        PurgeType(int nativeCode) {
            this.nativeCode = nativeCode;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    public static class TimeoutException extends IOException {}

    public static class Exception extends IOException {}

    public static void initNativeInterface(String baseDir) throws IOException {
        NativeSerialPort.initNativeInterface(baseDir);
    }

    public static String[] getPortNames() throws IOException {
        synchronized (SerialPort.class) {
            if (!inited) {
                initNativeInterface(null);
                inited = true;
            }

            String[] portNames;
            if ((portNames = NativeSerialPort.get_port_names()) == null)
                NativeSerialPort.throwNativeError();

            return portNames;
        }
    }

    private final String name;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    /*pp*/  final Semaphore semaphore;

    /*pp*/  long nativeSerialPort;
    private long readTimeout;
    private int baud;
    private DataBits dataBits;
    private Parity parity;
    private StopBits stopBits;

    public SerialPort(String name) throws IOException {
        synchronized (SerialPort.class) {
            if (!inited) {
                initNativeInterface(null);
                inited = true;
            }
        }

        if ((this.nativeSerialPort = NativeSerialPort.open(name)) == CLOSED_NATIVE_PORT)
            NativeSerialPort.throwNativeError();

        this.readTimeout = NativeSerialPort.get_read_timeout(this.nativeSerialPort);
        this.baud = NativeSerialPort.get_baud(this.nativeSerialPort);
        this.dataBits = DataBits.fromNativeCode(NativeSerialPort.get_data_bits(this.nativeSerialPort));
        this.parity = Parity.fromNativeCode(NativeSerialPort.get_parity(this.nativeSerialPort));
        this.stopBits = StopBits.fromNativeCode(NativeSerialPort.get_stop_bits(this.nativeSerialPort));

        this.name = name;
        this.inputStream = new SerialPortInputStream(this);
        this.outputStream = new SerialPortOutputStream(this);

        this.semaphore = new Semaphore(SEMAPHORE_MAX_PERMITS);
    }

    public boolean isOpen() {
        boolean result;
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);
            result = nativeSerialPort != CLOSED_NATIVE_PORT;
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);
            if (nativeSerialPort != CLOSED_NATIVE_PORT) {
                if (!NativeSerialPort.close(nativeSerialPort)) {
                    NativeSerialPort.throwNativeError();
                }

                nativeSerialPort = CLOSED_NATIVE_PORT;
            }
            semaphore.release(SEMAPHORE_MAX_PERMITS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e){
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            throw e;
        }
    }

    public void config(int baud, DataBits dataBits, Parity parity, StopBits stopBits) throws IOException {
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);

            if (nativeSerialPort == CLOSED_NATIVE_PORT)
                throw new IOException("Port is not open");

            if (!NativeSerialPort.config(nativeSerialPort, baud, dataBits.nativeCode, parity.nativeCode, stopBits.nativeCode))
                NativeSerialPort.throwNativeError();

            this.baud = baud;
            this.dataBits = dataBits;
            this.parity = parity;
            this.stopBits = stopBits;

            semaphore.release(SEMAPHORE_MAX_PERMITS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            throw e;
        }
    }

    public void purgeRX() throws IOException {
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);

            if (nativeSerialPort == CLOSED_NATIVE_PORT)
                throw new IOException("Port is not open");

            if (!NativeSerialPort.purge(nativeSerialPort, PurgeType.RX.nativeCode))
                NativeSerialPort.throwNativeError();

            semaphore.release(SEMAPHORE_MAX_PERMITS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            throw e;
        }
    }

    public void purgeTX() throws IOException {
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);

            if (nativeSerialPort == CLOSED_NATIVE_PORT)
                throw new IOException("Port is not open");

            if (!NativeSerialPort.purge(nativeSerialPort, PurgeType.TX.nativeCode))
                NativeSerialPort.throwNativeError();

            semaphore.release(SEMAPHORE_MAX_PERMITS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            throw e;
        }
    }

    public void setReadTimeout(long millis) throws IOException {
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);

            if (nativeSerialPort == CLOSED_NATIVE_PORT)
                throw new IOException("Port is not open");

            if (millis < 0)
                throw new IllegalArgumentException("Negative timeout");

            if (!NativeSerialPort.set_read_timeout(nativeSerialPort, millis))
                NativeSerialPort.throwNativeError();

            this.readTimeout = millis;
            semaphore.release(SEMAPHORE_MAX_PERMITS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            throw e;
        }
    }

    public String getName() {
        return name;
    }

    public int getBaud() {
        int baud;
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);
            baud = this.baud;
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            return baud;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public DataBits getDataBits() {
        DataBits dataBits;
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);
            dataBits = this.dataBits;
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            return dataBits;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Parity getParity() {
        Parity parity;
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);
            parity = this.parity;
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            return parity;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public StopBits getStopBits() {
        StopBits stopBits;
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);
            stopBits = this.stopBits;
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            return stopBits;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public long getReadTimeout() {
        long readTimeout;
        try {
            semaphore.acquire(SEMAPHORE_MAX_PERMITS);
            readTimeout = this.readTimeout;
            semaphore.release(SEMAPHORE_MAX_PERMITS);
            return readTimeout;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public String toString() {
        boolean open = (nativeSerialPort != CLOSED_NATIVE_PORT);
        return String.format("%s %d %d%s%s%s", name, baud, dataBits.nativeCode, parity.protocolStrToken, stopBits.protocolStrToken, open ? "" : " (closed)");
    }
}
