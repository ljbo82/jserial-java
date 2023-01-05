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

import java.util.regex.Pattern;

final class SemanticVersion implements Comparable<SemanticVersion> {
    private static void validate(int major, int minor, int patch, String extra) {
        if (major < 0)
            throw new IllegalArgumentException("Negative major");

        if (minor < 0)
            throw new IllegalArgumentException("Negative minor");

        if (patch < 0)
            throw new IllegalArgumentException("Negative patch");

        if (extra != null && extra.isEmpty())
            throw new IllegalArgumentException("Empty extra");
    }

    public final int major;
    public final int minor;
    public final int patch;
    public final String extra;

    public SemanticVersion(int major, int minor, int patch, String extra) {
        validate(major, minor, patch, extra);

        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.extra = extra;
    }

    public SemanticVersion(String version) {
        String[] tokens = version.split(Pattern.quote("."));
        int mMajor = Integer.parseInt(tokens[0]);
        int mMinor = tokens.length > 0 ? Integer.parseInt(tokens[1]) : 0;
        int mPatch;
        String mExtra;
        if (tokens.length > 2) {
            tokens = tokens[2].split(Pattern.quote("-"), 2);
            mPatch = Integer.parseInt(tokens[0]);
            mExtra = tokens.length > 1 ? tokens[1] : null;
        } else {
            mPatch = 0;
            mExtra = null;
        }

        validate(mMajor, mMinor, mPatch, mExtra);
        this.major = mMajor;
        this.minor = mMinor;
        this.patch = mPatch;
        this.extra = mExtra;
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d%s", major, minor, patch, extra == null ? "" : String.format("-%s", extra));
    }

    @Override
    public int compareTo(SemanticVersion other) {
        if (this.major < other.major)
            return -1;

        if (this.major > other.major)
            return 1;

        // this.major == other.major

        if (this.minor < other.minor)
            return -1;

        if (this.minor > other.minor)
            return 1;

        // this.minor == other.minor

        if (this.patch < other.patch)
            return -1;

        if (this.patch > other.patch)
            return 1;

        // this.patch == other.patch

        if (this.extra == null && other.extra != null)
            return -1;

        if (this.extra != null && other.extra == null)
            return 1;

        if (this.extra == null && other.extra == null)
            return 0;

        // this.extra != null && other.extra != null
        return this.extra.compareTo(other.extra);
    }
}
