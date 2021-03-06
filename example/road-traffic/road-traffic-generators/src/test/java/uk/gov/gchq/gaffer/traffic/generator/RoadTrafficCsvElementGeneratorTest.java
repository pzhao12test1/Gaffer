/*
 * Copyright 2017 Crown Copyright
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

package uk.gov.gchq.gaffer.traffic.generator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.data.generator.OneToManyElementGenerator;

import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RoadTrafficCsvElementGeneratorTest {

    @Test
    public void shouldParseSampleData() {

        // Given
        final OneToManyElementGenerator<CSVRecord> generator = new RoadTrafficCsvElementGenerator();

        try {
            final FileReader reader = new FileReader(getClass().getResource("/roadTrafficSampleData.csv").getFile());
            final Iterable<CSVRecord> csvRecords = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());

            // When
            final Iterable<? extends Element> elements = generator.apply(csvRecords);

            int entityCount = 0;
            int edgeCount = 0;
            for (final Element element : elements) {
                if (element instanceof Entity) {
                    entityCount++;
                } else if (element instanceof Edge) {
                    edgeCount++;
                } else {
                    fail("Unrecognised element class: " + element.getClassName());
                }
            }

            assertEquals(1600, entityCount);
            assertEquals(700, edgeCount);

        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

}
