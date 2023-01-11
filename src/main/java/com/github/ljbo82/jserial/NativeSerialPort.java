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

class NativeSerialPort {
    private static boolean inited;
    private static String  nativeHost;

    private NativeSerialPort() {}

    private static final int IO      = -3;
    private static final int ACCESS  = -4;
    private static final int TIMEOUT = -7;

    static synchronized void throwNativeError() throws IOException {
        initNativeInterface();

        int nativeCode = getCurrentError();
        clearCurrentError();

        String errorMsg;
        switch (nativeCode) {
            case TIMEOUT:
                throw new SerialPort.TimeoutException();

            case ACCESS:
                errorMsg = "Access error";
                break;

            case IO:
                errorMsg = "";
                break;

            default:
                errorMsg = String.format("Native error (nativeCode: %d)", nativeCode);
                break;
        }

        throw new IOException(errorMsg);
    }

    static synchronized String getNativeHost() {
        if (nativeHost != null) {
            return nativeHost;
        }

        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        boolean supported;

        if (osName.equals("Linux")) {
            osName = "linux";
            supported = true;
        } else if (osName.startsWith("Win")) {
            osName = "windows";
            supported = true;
        } else {
            supported = false;
        }

        if (supported) {
            if (osArch.equals("i386") || osArch.equals("i686") || osArch.equals("x86")) {
                osArch = "x86";
            } else if (osArch.equals("amd64") || osArch.equals("universal")) {
                osArch = "x64";
            } else {
                supported = false;
            }
        }

        if (!supported)
            throw new UnsupportedOperationException(String.format("Unsupported native host (os.name \"%s\", os.arch: \"%s\")", osName, osArch));

        nativeHost = String.format("%s-%s", osName, osArch);
        return nativeHost;
    }

    public static synchronized void initNativeInterface() throws IOException {
        if (inited)
            return;

        new NativeLibLoader(NativeSerialPort.class.getName()) {
            @Override
            protected String getNativeHost() {
                return NativeSerialPort.getNativeHost();
            }
        }
            .registerEmbeddedLib("linux-x64",   "/native/jserial-jni-x64.so")
            .registerEmbeddedLib("windows-x64", "/native/jserial-jni-x64.dll")
            .init();

        inited = true;
    }

    @SuppressWarnings("unused")
    public static native String getNativeLibVersion();

    public static native String[] getPortNames();

    public static native int getCurrentError();

    public static native void clearCurrentError();

    public static native long open(String portName);

    public static native boolean close(long nativePort);

    public static native boolean config(long nativePort, int baud, int dataBits, int parity, int stopBits);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static native boolean purge(long nativePort, int purgeType);

    public static native int getBaud(long nativePort);

    public static native int getDataBits(long nativePort);

    public static native int getParity(long nativePort);

    public static native int getStopBits(long nativePort);

    public static native boolean setReadTimeout(long nativePort, long millis);

    public static native long getReadTimeout(long nativePort);

    public static native int read(long nativePort, byte[] out, int off, int len);

    public static native boolean write(long nativePort, byte[] in, int off, int len);

    public static native boolean flush(long nativePort);
}
