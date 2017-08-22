/*
 * Copyright 2017. Crown Copyright
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

package uk.gov.gchq.gaffer.parquetstore.operation.addelements.impl;

import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.spark.TaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.data.element.comparison.ComparableOrToStringComparator;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.parquetstore.ParquetStore;
import uk.gov.gchq.gaffer.parquetstore.io.writer.ParquetElementWriter;
import uk.gov.gchq.gaffer.parquetstore.utils.ParquetStoreConstants;
import uk.gov.gchq.gaffer.parquetstore.utils.SchemaUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Takes an {@link Iterator} of {@link Element}'s and writes the elements out into Parquet files split into directories for each group.
 */
public class WriteUnsortedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteUnsortedData.class);
    private String tempFilesDir;
    private final SchemaUtils schemaUtils;
    private final Map<String, Map<Integer, ParquetWriter<Element>>> groupSplitToWriter;
    private final Map<String, Map<Integer, Object>> groupToSplitPoints;
    private final ComparableOrToStringComparator comparator;

    public WriteUnsortedData(final ParquetStore store, final Map<String, Map<Integer, Object>> groupToSplitPoints) {
        this(store.getTempFilesDir(), store.getSchemaUtils(), groupToSplitPoints);
    }

    public WriteUnsortedData(final String tempFilesDir, final SchemaUtils schemaUtils,
                             final Map<String, Map<Integer, Object>> groupToSplitPoints) {
        this.tempFilesDir = tempFilesDir;
        this.schemaUtils = schemaUtils;
        this.groupToSplitPoints = groupToSplitPoints;
        this.comparator = new ComparableOrToStringComparator();
        this.groupSplitToWriter = new HashMap<>();
    }

    public void writeElements(final Iterator<? extends Element> elements) throws OperationException {
        try {
            // Write elements
            _writeElements(elements);
            // Close the writers
            for (final Map<Integer, ParquetWriter<Element>> splitToWriter : groupSplitToWriter.values()) {
                for (final ParquetWriter<Element> writer : splitToWriter.values()) {
                    writer.close();
                }
            }
        } catch (final IOException | OperationException e) {
            throw new OperationException("Exception writing elements to temporary directory: " + tempFilesDir, e);
        }
    }

    private void _writeElements(final Iterator<? extends Element> elements) throws OperationException, IOException {
        while (elements.hasNext()) {
            final Element element = elements.next();
            final String group = element.getGroup();
            final ParquetWriter<Element> writer;
            final Map<Integer, ParquetWriter<Element>> splitToWriter;
            if  (groupSplitToWriter.containsKey(group)) {
                splitToWriter = groupSplitToWriter.get(group);
            } else {
                splitToWriter = new HashMap<>();
                groupSplitToWriter.put(group, splitToWriter);
            }
            if (schemaUtils.getEntityGroups().contains(group)) {
                writer = getWriter(splitToWriter, groupToSplitPoints.get(group), ((Entity) element).getVertex(), group, ParquetStoreConstants.VERTEX);
            } else {
                writer = getWriter(splitToWriter, groupToSplitPoints.get(group), ((Edge) element).getSource(), group, ParquetStoreConstants.SOURCE);
            }
            if (writer != null) {
                writer.write(element);
            } else {
                LOGGER.warn("Skipped the adding of an Element with Group = {} as that group does not exist in the schema.", group);
            }
        }
    }

    private ParquetWriter<Element> getWriter(final Map<Integer, ParquetWriter<Element>> splitToWriter,
                                             final Map<Integer, Object> splitPoints,
                                             final Object gafferObject, final String group, final String column) throws IOException {
        final int numOfSplits = splitPoints.size();
        for (int i = 1; i < numOfSplits; i++) {
            final Object splitPoint = splitPoints.get(i);
            final int comparision = comparator.compare(gafferObject, splitPoint);
            if (comparision < 0) {
                return _getWriter(splitToWriter, i - 1, group, column);
            }
        }
        if (numOfSplits == 1 && comparator.compare(gafferObject, splitPoints.get(0)) == 0) {
            return _getWriter(splitToWriter, 0, group, column);
        }
        return _getWriter(splitToWriter, numOfSplits - 1, group, column);
    }

    private ParquetWriter<Element> _getWriter(final Map<Integer, ParquetWriter<Element>> splitToWriter, final int i,
                                              final String group, final String column) throws IOException {
        final boolean isEntity = ParquetStoreConstants.VERTEX.equals(column);
        final ParquetWriter<Element> writer;
        if (!splitToWriter.containsKey(i)) {
            writer = buildWriter(group, column, isEntity, i);
            splitToWriter.put(i, writer);
        } else {
            writer = splitToWriter.get(i);
        }
        return writer;
    }

    private ParquetWriter<Element> buildWriter(final String group, final String column, final boolean isEntity, final int splitNumber) throws IOException {
        LOGGER.debug("Creating a new writer for group: {}", group + " with file number " + splitNumber);
        final Path filePath = new Path(ParquetStore.getGroupDirectory(group, column,
                tempFilesDir) + "/raw/split" + splitNumber + "/part-" + TaskContext.getPartitionId() + ".parquet");

        return new ParquetElementWriter.Builder(filePath)
                .isEntity(isEntity)
                .withType(schemaUtils.getParquetSchema(group))
                .usingConverter(schemaUtils.getConverter(group))
                .withCompressionCodec(CompressionCodecName.UNCOMPRESSED)
                .withSparkSchema(schemaUtils.getSparkSchema(group))
                .build();
    }
}
