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

package com.google.samples.exposurenotification.matching;

import com.google.samples.exposurenotification.storage.ContactRecordDataStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Pre-processes the local scanned IDs
 */
public class ContactRecordLookUpTable {
    private static final int INDEX_TABLE_SIZE = 65536;
    private final List<byte[]> sortedIds;
    private final int[] indexTable;

    private ContactRecordLookUpTable(List<byte[]> sortedIds, int[] indexTable) {
        this.sortedIds = sortedIds;
        this.indexTable = indexTable;
    }

    public static ContactRecordLookUpTable create(ContactRecordDataStore contactRecordDataStore) {
        List<byte[]> sortedIds = sortIds(contactRecordDataStore);
        return new ContactRecordLookUpTable(sortedIds, indexing(sortedIds));
    }

    public static ContactRecordLookUpTable createDefault() {
        return new ContactRecordLookUpTable(new ArrayList<>(), new int[0]);
    }

    private static List<byte[]> sortIds(ContactRecordDataStore contactRecordDataStore) {
        List<byte[]> ids = contactRecordDataStore.getAllRawIds();
        Collections.sort(
                ids,
                new Comparator<byte[]>() {
                    @Override
                    public int compare(byte[] lhs, byte[] rhs) {
                        // Compares first 2-byte, 1 - greater than, 0 - equal, -1 - less than
                        return Integer.compare(idToIndexValue(lhs), idToIndexValue(rhs));
                    }
                });
        return ids;
    }

    private static int[] indexing(List<byte[]> sortedIds) {
        int[] indexTable = new int[INDEX_TABLE_SIZE]; // 2-byte prefix as the index of the table.
        int lastIndex = 0;
        for (int i = 0; i < sortedIds.size(); i++) {
            int currentIndex = idToIndexValue(sortedIds.get(i));
            while (lastIndex < currentIndex) {
                indexTable[lastIndex++] = i;
            }
        }
        while (lastIndex < INDEX_TABLE_SIZE) {
            indexTable[lastIndex++] = sortedIds.size();
        }
        return indexTable;
    }

    private static int idToIndexValue(byte[] id) {
        return ((id[0] & 0xff) << 8) | (id[1] & 0xff);
    }

    public boolean find(byte[] id) {
        int index = idToIndexValue(id);
        int startIndex = (index > 0) ? indexTable[index - 1] : 0;
        int endIndex = indexTable[index];
        for (; startIndex < endIndex; startIndex++) {
            if (Arrays.equals(id, sortedIds.get(startIndex))) {
                return true;
            }
        }
        return false;
    }
}