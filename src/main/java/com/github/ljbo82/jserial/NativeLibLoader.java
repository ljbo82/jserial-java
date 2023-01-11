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
    private static InputStream getEmbeddedResource(String embeddedPath) {
        return NativeLibLoader.class.getResourceAsStream(embeddedPath);
    }

    @SuppressWarnings("UnusedReturnValue")
    private static boolean mkdir(File dir) throws IOException {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException(String.format("Error creating directory %s", dir.getAbsolutePath()));
            }

            return true;
        } else {
            if (!dir.isDirectory()) {
                throw new IOException(String.format("Path exists and it does not point to a directory: %s", dir.getAbsolutePath()));
            }

            return false;
        }
    }

    private static File createTmpDir(String name) throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));

        if (name != null && !name.isEmpty()) {
            tmpDir = new File(tmpDir, name);
            for (int i = 1; tmpDir.exists() && i <= 1000; i++) {
                tmpDir = new File(tmpDir.getParent(), String.format("%s_%d", name, i));
            }

            if (tmpDir.exists())
                throw new IOException("Too many attempts trying to create directory");

            mkdir(tmpDir);
        }

        return tmpDir;
    }

    private static File extractEmbeddedResource(String embeddedPath, File outputDir) throws IOException {
        try (InputStream is = getEmbeddedResource(embeddedPath)) {
            if (is == null) {
                throw new IOException(String.format("No such resource: %s", embeddedPath));
            }

            File libFile = new File(outputDir, embeddedPath);
            mkdir(libFile.getParentFile());

            //noinspection IOStreamConstructor
            try (OutputStream os = new FileOutputStream(libFile)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    os.write(buffer, 0, read);
                }
            }

            return libFile;
        }
    }

    private static void deleteDirectory(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException(String.format("Cannot delete file: %s", file.getAbsolutePath()));
                    }
                }
            }
        }

        if (!dir.delete()) {
            throw new IOException(String.format("Cannot delete directory: %s", dir.getAbsolutePath()));
        }
    }
    // =================================================================================================================
    // endregion

    private final Map<String, Set<String>> libMap = new HashMap<>();
    private final String tmpDirName;

    private boolean inited;

    @SuppressWarnings("unused")
    public NativeLibLoader() {
        this(null);
    }

    public NativeLibLoader(String tmpDirName) {
        this.tmpDirName = tmpDirName;
    }

    @SuppressWarnings("unused")
    public final String getTmpDirName() {
        return tmpDirName;
    }

    public synchronized final NativeLibLoader registerEmbeddedLib(String host, String embeddedLibPath) throws IllegalStateException {
        if (inited)
            throw new IllegalStateException("Already initialized");

        if (host == null)
            throw new IllegalArgumentException("host cannot be null");

        if (embeddedLibPath == null || embeddedLibPath.isEmpty())
            throw new IllegalArgumentException("Null/Empty embedded library path");

        Set<String> libs = libMap.get(host);
        //noinspection Java8MapApi
        if (libs == null) {
            libs = new LinkedHashSet<>();
            libMap.put(host, libs);
        }

        if (!libs.add(embeddedLibPath))
            throw new IllegalStateException(String.format("Embedded library already registered for %s: %s", host, embeddedLibPath));

        return this;
    }

    protected abstract String getNativeHost();

    public synchronized final void init() throws IOException {
        if (inited)
            return;

        String nativeHost = getNativeHost();

        Set<String> embeddedLibPaths = libMap.get(nativeHost);
        if (embeddedLibPaths == null)
            return;

        File tmpDir = createTmpDir(tmpDirName);
        if (tmpDirName != null && !tmpDirName.isEmpty()) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    deleteDirectory(tmpDir);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (String embeddedLibPath : embeddedLibPaths) {
            File libFile = extractEmbeddedResource(embeddedLibPath, tmpDir);
            System.load(libFile.getAbsolutePath());
        }

        inited = true;
    }
}
