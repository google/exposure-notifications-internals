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

package com.google.samples.exposurenotification.storage;

import androidx.annotation.NonNull;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Provides utility functions for {@link CloseableIterable}.
 */
public final class CloseableIterables {
    /**
     * A simple implementation of concatenating multiple {@link CloseableIterable}. The returned
     * iterable will iterate the first {@link CloseableIterable} in {@code in iterables} and then the
     * second, etc.
     */
    public static <T> CloseableIterable<T> concat(List<CloseableIterable<T>> closeableIterables) {
        if (closeableIterables.isEmpty()) {
            return emptyInstance();
        }

        return new CloseableIterable<T>() {
            @NonNull
            @Override
            public Iterator<T> iterator() {
                return Iterators.concat(
                        Iterables.transform(closeableIterables, CloseableIterable::iterator).iterator());
            }

            @Override
            public void close() throws IOException {
                for (CloseableIterable<T> closeableIterable : closeableIterables) {
                    closeableIterable.close();
                }
            }
        };
    }

    public static <T> CloseableIterable<T> emptyInstance() {
        return new CloseableIterable<T>() {
            @Override
            public void close() throws IOException {
            }

            @NonNull
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public T next() {
                        throw new NoSuchElementException();
                    }
                };
            }
        };
    }

    private CloseableIterables() {
    }
}