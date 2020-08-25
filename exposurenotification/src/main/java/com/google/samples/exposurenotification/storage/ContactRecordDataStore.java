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

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.common.collect.Iterables;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.samples.exposurenotification.Log;
import com.google.samples.exposurenotification.ScannedPacket;
import com.google.samples.exposurenotification.ScannedPacket.ScannedPacketBuilder;
import com.google.samples.exposurenotification.ScannedPacket.ScannedPacketContent;
import com.google.samples.exposurenotification.ScannedPacket.ScannedPacketContent.ScannedPacketContentBuilder;
import com.google.samples.exposurenotification.data.DayNumber;
import com.google.samples.exposurenotification.data.RollingProximityId;
import com.google.samples.exposurenotification.features.ContactTracingFeature;

import org.joda.time.Instant;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * The data store for contact records. Contact records are bundled BLE scan results stored in
 * key-value pairs. The key is a concatenation of {@link DayNumber} and {@link RollingProximityId}.
 * The value is a {@link ContactRecordValue};
 */
@SuppressWarnings("NewApi")
public class ContactRecordDataStore implements AutoCloseable {

    private final Map<byte[], byte[]> store = new HashMap<>();

    private ContactRecordDataStore(Context context) throws StorageException {
    }

    public static synchronized ContactRecordDataStore open(Context context) throws StorageException {
        return new ContactRecordDataStore(context);
    }

    @Override
    public synchronized void close() {
    }

    /**
     * Gets the contact records that match the given {@code dayNumber} and {@code rollingProximityId}.
     *
     * @return the record if a match is found, or null otherwise.
     */
    @Nullable
    public ContactRecord getRecord(DayNumber dayNumber, RollingProximityId rollingProximityId) {
        ContactRecordKey key = new ContactRecordKey(dayNumber, rollingProximityId);
        try {
            byte[] valueBytes = store.get(key);
            if (valueBytes == null) {
                return null;
            }
            ContactRecordValue parsedValue = ContactRecordValue.parseFrom(valueBytes);
            return new ContactRecord(
                    key,
                    ContactTracingFeature.contactRecordStoreCompactFormatEnabled()
                            ? convertToCompactFormat(parsedValue)
                            : parsedValue);
        } catch (InvalidProtocolBufferException e) {
            Log.log.atSevere().withCause(e).log("Error getting record");
        }
        return null;
    }

    public List<ContactRecord> getAllRecords() {
        List<ContactRecord> records = new ArrayList<>();
        synchronized (store) {
            try {
                for (Entry<byte[], byte[]> iterator : store.entrySet()) {
                    if (iterator.getKey() == null) {
                        continue;
                    }
                    ContactRecordValue parsedValue =
                            ContactRecordValue.parseFrom(iterator.getValue());
                    records.add(
                            new ContactRecord(
                                    ContactRecordKey.fromBytes(iterator.getKey()),
                                    ContactTracingFeature.contactRecordStoreCompactFormatEnabled()
                                            ? convertToCompactFormat(parsedValue)
                                            : parsedValue));
                }
            } catch (InvalidProtocolBufferException e) {
                Log.log.atSevere().withCause(e).log("Error fetching record");
            }
        }
        return records;
    }

    /**
     * Gets all scan records in {@link ScannedPacket} format, to pass the records from wearable to
     * phone. Should be used in wearable EN module only.
     */
    public List<ScannedPacket> getAllRecordsFromWearable() {
        ArrayList<ScannedPacket> scannedPacketArrayList = new ArrayList<>();
        List<ContactRecord> contactRecordList = getAllRecords();
        Log.log
                .atInfo()
                .log(
                        "getAllRecordsFormWearable called, contactRecordList.size=%d",
                        contactRecordList.size());
        for (ContactRecord contactRecord : contactRecordList) {
            if (contactRecord.getValue().getSightingRecordsCount() <= 0) {
                continue;
            }
            ArrayList<ScannedPacketContent> scannedPacketContents = new ArrayList<>();
            byte[] metadata = null;
            for (SightingRecord sightingRecord : contactRecord.value.getSightingRecordsList()) {
                if (metadata == null) {
                    metadata = sightingRecord.getAssociatedEncryptedMetadata().toByteArray();
                }
                scannedPacketContents.add(
                        new ScannedPacketContentBuilder()
                                .setEpochSeconds(sightingRecord.getEpochSeconds())
                                .setRssi(sightingRecord.getRssi())
                                .setPreviousScanEpochSeconds(sightingRecord.getPreviousScanEpochSeconds())
                                .build());
            }
            if (metadata == null) {
                continue;
            }
            scannedPacketArrayList.add(
                    new ScannedPacketBuilder()
                            .setId(contactRecord.key.rollingProximityId.getDirect())
                            .setEncryptedMetadata(metadata)
                            .setScannedPacketContents(scannedPacketContents.toArray(new ScannedPacketContent[0]))
                            .build());
        }
        Log.log
                .atInfo()
                .log(
                        "getAllRecordsFormWearable done, %d scanned packet found",
                        scannedPacketArrayList.size());
        return scannedPacketArrayList;
    }

    /**
     * Gets all the scanned IDs, for each contact record, only return the 16-byte raw ID.
     */
    public List<byte[]> getAllRawIds() {
        List<byte[]> rawIds = new ArrayList<>();
        synchronized (store) {
            for (Entry<byte[], byte[]> iterator : store.entrySet()) {
                if (iterator.getKey() == null) {
                    continue;
                }
                rawIds.add(ContactRecordKey.getRollingProximityId(iterator.getKey()));
            }
        }
        return rawIds;
    }

    /**
     * Adds or updates a contact record value with the key given by {@code dayNumber} and {@code
     * rollingProximityId}.
     */
    @VisibleForTesting
    public void putRecord(
            DayNumber dayNumber, RollingProximityId rollingProximityId, ContactRecordValue value) {
        ContactRecordKey key = new ContactRecordKey(dayNumber, rollingProximityId);
        synchronized (store) {
            store.put(key.getBytes(), value.toByteArray());
        }
    }

    /**
     * Appends a {@link SightingRecord} to the {@link ContactRecordValue} keyed by the {@code
     * dayNumber} and {@code rollingProximityId}.
     */
    public void appendSightingRecord(
            Instant sightTime,
            byte[] id,
            int rssi,
            byte[] associatedEncryptedMetadata,
            int previousScanEpochSeconds) {
        DayNumber dayNumber = new DayNumber(sightTime);
        int sightTimeSeconds = (int) MILLISECONDS.toSeconds(sightTime.getMillis());
        RollingProximityId rollingProximityId = new RollingProximityId(id);

        if (!ContactTracingFeature.contactRecordStoreCompactFormatEnabled()) {
            SightingRecord sightingRecord =
                    SightingRecord.newBuilder()
                            .setEpochSeconds(sightTimeSeconds)
                            .setRssi(rssi)
                            .setAssociatedEncryptedMetadata(ByteString.copyFrom(associatedEncryptedMetadata))
                            .setPreviousScanEpochSeconds(previousScanEpochSeconds)
                            .build();
            ContactRecord contactRecord = getRecord(dayNumber, rollingProximityId);
            ContactRecordValue contactRecordValue =
                    contactRecord == null
                            ? ContactRecordValue.getDefaultInstance()
                            : contactRecord.getValue();
            ContactRecordValue updatedContactRecordValue =
                    contactRecordValue.toBuilder().addSightingRecords(sightingRecord).build();
            putRecord(dayNumber, rollingProximityId, updatedContactRecordValue);
            return;
        }

        ContactRecord contactRecord = getRecord(dayNumber, rollingProximityId);
        ContactRecordValue existingValue =
                contactRecord == null ? null : convertToCompactFormat(contactRecord.getValue());
        ContactRecordValue updatedValue;
        if (existingValue == null || existingValue.getSightingRecordsCount() == 0) {
            // No records for this RPI, or the data is corrupted. We create new ContactRecordValue
            // completely.
            updatedValue =
                    ContactRecordValue.newBuilder()
                            .setEncryptedMetadata(ByteString.copyFrom(associatedEncryptedMetadata))
                            .addSightingRecords(
                                    SightingRecord.newBuilder()
                                            .setEpochSeconds(sightTimeSeconds)
                                            .setPreviousScanEpochSeconds(previousScanEpochSeconds)
                                            .setRssiValues(wrapSingleRssi(rssi)))
                            .build();
        } else {
            SightingRecord.Builder sightingRecordBuilder =
                    Iterables.getLast(existingValue.getSightingRecordsList()).toBuilder();
            if (isSameScanCycle(sightingRecordBuilder, sightTimeSeconds)) {
                sightingRecordBuilder.setRssiValues(
                        sightingRecordBuilder.getRssiValues().concat(wrapSingleRssi(rssi)));
                backFillEncryptedMetadataIfRequired(
                        sightingRecordBuilder,
                        existingValue.getEncryptedMetadata(),
                        ByteString.copyFrom(associatedEncryptedMetadata));
                updatedValue =
                        existingValue.toBuilder()
                                .setSightingRecords(
                                        existingValue.getSightingRecordsCount() - 1, sightingRecordBuilder)
                                .build();
            } else {
                updatedValue =
                        existingValue.toBuilder()
                                .addSightingRecords(
                                        SightingRecord.newBuilder()
                                                .setEpochSeconds(sightTimeSeconds)
                                                .setPreviousScanEpochSeconds(previousScanEpochSeconds)
                                                .setRssiValues(wrapSingleRssi(rssi)))
                                .build();
            }
        }

        putRecord(dayNumber, rollingProximityId, updatedValue);
    }

    private static boolean isSameScanCycle(
            SightingRecord.Builder sightingRecordBuilder, int sightingTimeSeconds) {
        // Simple heuristic that considering a new sighting packet to be in the same scan cycle of a
        // SightingRecord if the elapsed seconds since the beginning of SightingRecord is no bigger than
        // 1.5x maximum possible scan duration.
        return sightingTimeSeconds
                <= sightingRecordBuilder.getEpochSeconds()
                + (ContactTracingFeature.scanTimeSeconds()
                + ContactTracingFeature.scanTimeExtendForProfileInUseSeconds())
                * 1.5;
    }

    /**
     * Ensures that `sightingRecordBuilder.encryptedMetadata` has the same (or larger) length as
     * `sightingRecordBuilder.rssiValues`. If not, fill the slots with the `encryptedMetadata`.
     */
    private static void fillMissingEncryptedMetadata(
            SightingRecord.Builder sightingRecordBuilder, ByteString encryptedMetadata) {
        if (!sightingRecordBuilder.hasRssiValues()) {
            return;
        }
        while (sightingRecordBuilder.getEncryptedMetadataCount()
                < sightingRecordBuilder.getRssiValues().size()) {
            sightingRecordBuilder.addEncryptedMetadata(encryptedMetadata);
        }
    }

    @VisibleForTesting
    static ByteString wrapSingleRssi(int rssi) {
        return ByteString.copyFrom(new byte[]{(byte) rssi});
    }

    /**
     * Back fills {@link SightingRecord.Builder#getEncryptedMetadataList()} per format requirement: If
     * {@code currentEncryptedMetadata} is not equal to {@code baseEncryptedMetadata}, we must fill
     * encrypted metadata for this sighting packet as well as all previous packets if absent. If not
     * equal, we do not need to fill it as long as the whole {@link
     * SightingRecord.Builder#getEncryptedMetadataList()} is empty, otherwise we still need to set it.
     *
     * @param sightingRecordBuilder    the builder to update
     * @param baseEncryptedMetadata    the encrypted metadata in {@link ContactRecordValue}.
     * @param currentEncryptedMetadata the encrypted metadata associated with the last element of
     *                                 {@link SightingRecord.Builder#getRssiValues()}.
     */
    private static void backFillEncryptedMetadataIfRequired(
            SightingRecord.Builder sightingRecordBuilder,
            ByteString baseEncryptedMetadata,
            ByteString currentEncryptedMetadata) {
        if (sightingRecordBuilder.getEncryptedMetadataCount() != 0
                || !currentEncryptedMetadata.equals(baseEncryptedMetadata)) {
            fillMissingEncryptedMetadata(sightingRecordBuilder, baseEncryptedMetadata);
            sightingRecordBuilder.setEncryptedMetadata(
                    sightingRecordBuilder.getEncryptedMetadataCount() - 1, currentEncryptedMetadata);
        }
    }

    /**
     * Coverts a {@link ContactRecordValue} to the "compact format" if it is not already in that
     * format.
     *
     * <p>We can remove the conversion code once the build using compact format has been rolled out
     * for a while.
     */
    private static ContactRecordValue convertToCompactFormat(ContactRecordValue originalValue) {
        if (originalValue.hasEncryptedMetadata()) {
            return originalValue;
        }

        ContactRecordValue.Builder updatedValueBuilder = ContactRecordValue.newBuilder();
        SightingRecord.Builder sightingRecordBuilder = null;
        for (SightingRecord sightingRecord : originalValue.getSightingRecordsList()) {
            // Set the base encrypted metadata to be the first one we see from the list.
            if (!updatedValueBuilder.hasEncryptedMetadata()) {
                updatedValueBuilder.setEncryptedMetadata(sightingRecord.getAssociatedEncryptedMetadata());
            }

            if (sightingRecordBuilder != null
                    && isSameScanCycle(sightingRecordBuilder, sightingRecord.getEpochSeconds())) {
                sightingRecordBuilder.setRssiValues(
                        sightingRecordBuilder.getRssiValues().concat(wrapSingleRssi(sightingRecord.getRssi())));
            } else {
                if (sightingRecordBuilder != null) {
                    updatedValueBuilder.addSightingRecords(sightingRecordBuilder);
                }
                sightingRecordBuilder =
                        sightingRecord.toBuilder()
                                .setRssiValues(wrapSingleRssi(sightingRecord.getRssi()))
                                .clearAssociatedEncryptedMetadata()
                                .clearRssi();
            }
            backFillEncryptedMetadataIfRequired(
                    sightingRecordBuilder,
                    updatedValueBuilder.getEncryptedMetadata(),
                    sightingRecord.getAssociatedEncryptedMetadata());
        }
        if (sightingRecordBuilder != null) {
            updatedValueBuilder.addSightingRecords(sightingRecordBuilder);
        }

        return updatedValueBuilder.build();
    }

    /**
     * Deletes all contact records on a day given by the {@link DayNumber}.
     *
     * @return the total number of entries deleted
     */
    public int deletesRecords(DayNumber dayNumber) throws StorageException {
        // FIXME: delete records for a given day number.
        return 0;
    }

    /**
     * Deletes all contact records prior (inclusive) to a day given by the {@link DayNumber}.
     *
     * @return the total number of entries deleted
     */
    public int deletePriorRecords(DayNumber lastDayNumberToDelete) throws StorageException {
        // FIXME: Delete prescribed records.
        return 0;
    }

    /**
     * An immutable data class represents a contact record. The key-value pair data can be accessed
     * through {@link #getKey()} and {@link #getValue()}.
     */
    public static class ContactRecord {
        private final ContactRecordKey key;
        private final ContactRecordValue value;

        public ContactRecord(ContactRecordKey key, ContactRecordValue value) {
            this.key = key;
            this.value = value;
        }

        public ContactRecordKey getKey() {
            return key;
        }

        public ContactRecordValue getValue() {
            return value;
        }
    }

    /**
     * An immutable data class represents the key for a {@link ContactRecord}. The byte array
     * representation of this class is used as the key of underlying data store.
     */
    public static class ContactRecordKey {
        private final DayNumber dayNumber;
        private final RollingProximityId rollingProximityId;

        public ContactRecordKey(DayNumber dayNumber, RollingProximityId rollingProximityId) {
            this.dayNumber = dayNumber;
            this.rollingProximityId = rollingProximityId;
        }

        public DayNumber getDayNumber() {
            return dayNumber;
        }

        public RollingProximityId getRollingProximityId() {
            return rollingProximityId;
        }

        private static byte[] getRollingProximityId(byte[] contactRecordKeyBytes) {
            return Arrays.copyOfRange(
                    contactRecordKeyBytes, DayNumber.getSizeBytes(), contactRecordKeyBytes.length);
        }

        /**
         * Gets the byte array representation of the key.
         */
        public byte[] getBytes() {
            ByteBuffer byteBuffer =
                    ByteBuffer.allocate(DayNumber.getSizeBytes() + rollingProximityId.length);

            dayNumber.putIn(byteBuffer);
            rollingProximityId.putIn(byteBuffer);

            return byteBuffer.array();
        }

        public static ContactRecordKey fromBytes(byte[] bytes) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            DayNumber dayNumber = DayNumber.getFrom(byteBuffer);
            byte[] rollingProximityIdBytes = new byte[RollingProximityId.MIN_ID.length];
            byteBuffer.get(rollingProximityIdBytes);
            return new ContactRecordKey(dayNumber, new RollingProximityId(rollingProximityIdBytes, true));
        }
    }
}