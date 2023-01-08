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

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

abstract class NativeLibLoader {

    // region STATIC SCOPE
    // =================================================================================================================
    public static class InvalidLibException extends Exception {
        private final File libFile;
        private final String nativeHost;

        private InvalidLibException(String nativeHost, File libFile) {
            super(String.format("Invalid lib for %s: %s", nativeHost, libFile.getAbsolutePath()));

            this.nativeHost = nativeHost;
            this.libFile = libFile;
        }

        public String getNativeHost() {
            return nativeHost;
        }

        public File getLibFile() {
            return libFile;
        }
    }

    private static void extractEmbeddedResource(String embeddedPath, String outputDirPath) throws IOException {
        createDir(outputDirPath);
        File libFile = new File(outputDirPath, new File(embeddedPath).getName());

        if (libFile.exists())
            return;

        try (OutputStream os = new FileOutputStream(libFile)) {
            byte[] buffer = new byte[1024];
            try (InputStream is = getEmbeddedResource(embeddedPath)) {
                int read;
                while ((read = is.read(buffer)) > 0) {
                    os.write(buffer, 0, read);
                }
            }
        }
    }

    private static InputStream getEmbeddedResource(String embeddedPath) {
        return NativeLibLoader.class.getResourceAsStream(embeddedPath);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Win");
    }

    private static void createDir(String path) throws IOException {
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException(String.format("Error creating directory %s", dir.getAbsolutePath()));
            } else {
                if (dir.getName().startsWith(".") && isWindows()) {
                    Process p = Runtime.getRuntime().exec(String.format("attrib +H %s", dir.getAbsolutePath()));
                    try {
                        p.waitFor();
                        if (p.exitValue() != 0) {
                            throw new IOException(String.format("attrib error: %d", p.exitValue()));
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            if (!dir.isDirectory()) {
                throw new IOException(String.format("Path exists and it does not point to a directory: %s", dir.getAbsolutePath()));
            }
        }
    }
    // =================================================================================================================
    // endregion

    private final Map<String, Set<String>> libMap = new HashMap<>();

    private boolean inited;

    public NativeLibLoader registerNativeLib(String host, String embeddedLibPath) {
        if (host == null)
            throw new IllegalArgumentException("host cannot be null");

        if (embeddedLibPath == null || embeddedLibPath.isEmpty())
            throw new IllegalArgumentException("Null/Empty embedded library path");

        Set<String> libs = libMap.get(host);
        if (libs == null) {
            libs = new LinkedHashSet<>();
            libMap.put(host, libs);
        }

        if (!libs.add(embeddedLibPath))
            throw new IllegalStateException(String.format("Embedded library already registered for %s: %s", host, embeddedLibPath));

        return this;
    }

    protected abstract String getNativeHost();

    protected abstract boolean checkNativeLib(String nativeHost, File libFile);

    public synchronized void init(String outputDirPath) throws IllegalStateException, IOException, InvalidLibException {
        if (inited)
            throw new IllegalStateException("Already initialized");

        String nativeHost = getNativeHost();

        Set<String> embeddedLibPaths = libMap.get(nativeHost);
        if (embeddedLibPaths == null)
            return;

        File outputDir = new File(outputDirPath);
        createDir(outputDir.getAbsolutePath());

        for (String embeddedLibPath : embeddedLibPaths) {
            File libFile = new File(outputDir, embeddedLibPath);
            File parentDir = libFile.getParentFile();
            createDir(parentDir.getAbsolutePath());

            if (!libFile.exists())
                extractEmbeddedResource(embeddedLibPath, parentDir.getAbsolutePath());

            System.load(libFile.getAbsolutePath());

            if (!checkNativeLib(nativeHost, libFile)) {
                throw new InvalidLibException(nativeHost, libFile);
            }
        }

        inited = true;
    }
}
