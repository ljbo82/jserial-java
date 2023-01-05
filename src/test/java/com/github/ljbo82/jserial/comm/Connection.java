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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class Connection implements Closeable {

    public static class Wrapper extends Connection {
        private final Connection wrapped;

        public Wrapper(Connection wrapped) {
            if (wrapped == null)
                throw new NullPointerException("Wrapped instance cannot be null");

            this.wrapped = wrapped;
        }

        @Override
        public boolean isOpen() {
            return wrapped.isOpen();
        }

        @Override
        protected InputStream getInputStream() {
            return wrapped.getInputStream();
        }

        @Override
        protected OutputStream getOutputStream() {
            return wrapped.getOutputStream();
        }

        @Override
        public void purge() throws IOException {
            wrapped.purge();
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
        }

        @Override
        public String toString() {
            return wrapped.toString();
        }
    }

    public abstract boolean isOpen();

    protected abstract InputStream getInputStream();

    protected abstract OutputStream getOutputStream();

    public abstract void purge() throws IOException;
}
