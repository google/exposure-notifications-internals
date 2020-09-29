/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.exposurenotification.nearby;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.List;

/**
 * Provider which holds a list of diagnosis key files and can open/supply them one by one as they
 * are ready to be processed.
 */
public class DiagnosisKeyFileProvider {

    private int index = 0;
    private final List<File> files;

    public DiagnosisKeyFileProvider(List<File> files) {
        this.files = ImmutableList.copyOf(files);
    }

    /**
     * Checks whether another File is available.
     *
     * @hide
     */
    public boolean hasNext() {
        return files.size() > index;
    }

    /**
     * Gets the current file and increments the pointer to the next.
     *
     * @hide
     */
    public File getNext() {
        return files.get(index++);
    }
}