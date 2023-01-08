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

import java.io.File;
import java.io.IOException;

class NativeSerialPort {
    private static final String DEFAULT_BASE_DIR;
    static {
        DEFAULT_BASE_DIR = new File(System.getProperty("user.home"), ".jserial").getAbsolutePath();
    }

    private static boolean inited;
    private static String  nativeHost;

    private NativeSerialPort() {}

    private static final int IO      = -3;
    private static final int ACCESS  = -4;
    private static final int TIMEOUT = -7;

    public static void initNativeInterface(String outputDirPath) throws IOException {
        synchronized (NativeSerialPort.class) {
            if (inited)
                throw new IllegalStateException("Native interface already initialized");

            try {
                new NativeLibLoader() {
                    private final SemanticVersion MIN_INCLUDED_VERSION = new SemanticVersion("0.1.0");
                    private final SemanticVersion MAX_EXCLUDED_VERSION = new SemanticVersion("1.0.0");

                    @Override
                    protected String getNativeHost() {
                        return NativeSerialPort.getNativeHost();
                    }

                    @Override
                    protected boolean checkNativeLib(String nativeHost, File libFile) {
                        SemanticVersion nativeVersion = new SemanticVersion(get_native_lib_version());
                        return nativeVersion.compareTo(MIN_INCLUDED_VERSION) >= 0 && nativeVersion.compareTo(MAX_EXCLUDED_VERSION) < 0;
                    }
                }
                    .registerNativeLib("linux-x64",   "/native/jserial-jni-x64.so")
                    .registerNativeLib("windows-x64", "/native/jserial-jni-x64.dll")
                    .init(outputDirPath == null ? DEFAULT_BASE_DIR : outputDirPath);
            } catch (NativeLibLoader.InvalidLibException e) {
                throw new RuntimeException(e);
            }

            inited = true;
        }
    }

    public static native String get_native_lib_version();

    public static native String[] get_port_names();

    public static native int get_current_error();

    public static native void clear_current_error();

    public static native long open(String portName);

    public static native boolean close(long nativePort);

    public static native boolean config(long nativePort, int baud, int dataBits, int parity, int stopBits);

    public static native boolean purge(long nativePort, int purgeType);

    public static native int get_baud(long nativePort);

    public static native int get_data_bits(long nativePort);

    public static native int get_parity(long nativePort);

    public static native int get_stop_bits(long nativePort);

    public static native boolean set_read_timeout(long nativePort, long millis);

    public static native long get_read_timeout(long nativePort);

    public static native int read(long nativePort, byte[] out, int off, int len);

    public static native boolean write(long nativePort, byte[] in, int off, int len);

    public static native boolean flush(long nativePort);

    public static void throwNativeError() throws IOException {
        int nativeCode = get_current_error();
        clear_current_error();

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

    public static String getNativeHost() {
        synchronized (NativeSerialPort.class) {
            if (nativeHost != null) {
                return nativeHost;
            }

            String osName = System.getProperty("os.name");
            String osArch = System.getProperty("os.arch");

            boolean supported = false;

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
                throw new UnsupportedOperationException(String.format("Unsupported native host (os.name \"%s\", os.arch: \"%s\")"));

            nativeHost = String.format("%s-%s", osName, osArch);
            return nativeHost;
        }
    }
}
