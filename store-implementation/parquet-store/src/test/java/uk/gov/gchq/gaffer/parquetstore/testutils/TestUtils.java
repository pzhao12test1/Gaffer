package uk.gov.gchq.gaffer.parquetstore.testutils;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import uk.gov.gchq.gaffer.commonutil.CommonTestConstants;
import uk.gov.gchq.gaffer.commonutil.StreamUtil;
import uk.gov.gchq.gaffer.commonutil.TestTypes;
import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.parquetstore.ParquetStoreProperties;
import uk.gov.gchq.gaffer.parquetstore.operation.AbstractSparkOperationsTest;
import uk.gov.gchq.gaffer.parquetstore.utils.ParquetStoreConstants;
import uk.gov.gchq.gaffer.spark.SparkConstants;
import uk.gov.gchq.gaffer.store.SerialisationFactory;
import uk.gov.gchq.gaffer.store.schema.Schema;
import uk.gov.gchq.gaffer.store.schema.SchemaOptimiser;
import uk.gov.gchq.gaffer.types.FreqMap;
import uk.gov.gchq.gaffer.types.TypeValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

public class TestUtils {
    public static TreeSet<String> MERGED_TREESET = getMergedTreeSet();
    public static FreqMap MERGED_FREQMAP = getMergedFreqMap();
    public static Date DATE = new Date();
    public static Date DATE1 = new Date(TestUtils.DATE.getTime() + 1000);

    public static SparkSession spark = SparkSession.builder()
            .appName("Parquet Gaffer Store tests")
            .master(getParquetStoreProperties().getSparkMaster())
            .config(SparkConstants.DRIVER_ALLOW_MULTIPLE_CONTEXTS, "true")
            .config(SparkConstants.SERIALIZER, SparkConstants.DEFAULT_SERIALIZER)
            .config(SparkConstants.KRYO_REGISTRATOR, SparkConstants.DEFAULT_KRYO_REGISTRATOR)
            .getOrCreate();
    public static JavaSparkContext javaSparkContext = JavaSparkContext.fromSparkContext(spark.sparkContext());

    public static ParquetStoreProperties getParquetStoreProperties() {
        final ParquetStoreProperties parquetStoreProperties = ParquetStoreProperties.loadStoreProperties(
                AbstractSparkOperationsTest.class.getResourceAsStream("/multiUseStore.properties"));
        parquetStoreProperties.setTempFilesDir(CommonTestConstants.TMP_DIRECTORY.getAbsolutePath());
        return parquetStoreProperties;
    }

    public static Schema gafferSchema(final String schemaFolder) {
        final Schema schema = Schema.fromJson(StreamUtil.openStreams(TestUtils.class, schemaFolder));
        final SchemaOptimiser schemaOptimiser = new SchemaOptimiser(new SerialisationFactory(ParquetStoreConstants.SERIALISERS));
        return schemaOptimiser.optimise(schema, true);
    }

    private static TreeSet<String> getMergedTreeSet() {
        final TreeSet<String> t = new TreeSet<>();
        t.add("A");
        t.add("B");
        t.add("C");
        return t;
    }

    public static TreeSet<String> getTreeSet1() {
        final TreeSet<String> t = new TreeSet<>();
        t.add("A");
        t.add("B");
        return t;
    }

    public static TreeSet<String> getTreeSet2() {
        final TreeSet<String> t = new TreeSet<>();
        t.add("A");
        t.add("C");
        return t;
    }

    private static FreqMap getMergedFreqMap() {
        final FreqMap f = new FreqMap();
        f.upsert("A", 2L);
        f.upsert("B", 1L);
        f.upsert("C", 1L);
        return f;
    }

    public static FreqMap getFreqMap1() {
        final FreqMap f = new FreqMap();
        f.upsert("A", 1L);
        f.upsert("B", 1L);
        return f;
    }

    public static FreqMap getFreqMap2() {
        final FreqMap f = new FreqMap();
        f.upsert("A", 1L);
        f.upsert("C", 1L);
        return f;
    }

    public static List<Element> convertLongRowsToElements(final Dataset<Row> data) {
        final List<Element> elementList = new ArrayList<>((int) data.count());
        for (final Row row : data.collectAsList()) {
            final Element e;
            if (row.get(0) == null) {
                // generate Entity
                e = new Entity(row.getString(14), row.get(13));
            } else {
                //generate Edge
                e = new Edge(row.getString(14), row.get(0), row.get(1), row.getBoolean(2));
            }
            e.putProperty("byte", ((byte[]) row.get(3))[0]);
            e.putProperty("double", row.getDouble(4));
            e.putProperty("float", row.getFloat(5));
            e.putProperty("treeSet", new TreeSet<String>(row.getList(6)));
            e.putProperty("long", row.getLong(7));
            e.putProperty("short", ((Integer) row.getInt(8)).shortValue());
            e.putProperty("date", new Date(row.getLong(9)));
            e.putProperty("freqMap", new FreqMap(row.getJavaMap(10)));
            e.putProperty("count", row.getInt(11));
            e.putProperty(TestTypes.VISIBILITY, row.getString(12));
            elementList.add(e);
        }
        return elementList;
    }

    public static List<Element> convertStringRowsToElements(final Dataset<Row> data) {
        final List<Element> elementList = new ArrayList<>((int) data.count());
        for (final Row row : data.collectAsList()) {
            final Element e;
            if (row.get(0) == null) {
                // generate Entity
                e = new Entity(row.getString(14), row.get(13));
            } else {
                //generate Edge
                e = new Edge(row.getString(14), row.get(0), row.get(1), row.getBoolean(2));
            }
            e.putProperty("count", row.getInt(11));
            e.putProperty("visibility", row.getString(12));
            elementList.add(e);
        }
        return elementList;
    }

    public static List<Element> convertTypeValueRowsToElements(final Dataset<Row> data) {
        final List<Element> elementList = new ArrayList<>((int) data.count());
        for (final Row row : data.collectAsList()) {
            final Element e;
            if (row.get(0) == null) {
                // generate Entity
                e = new Entity(row.getString(17), new TypeValue(row.getString(15), row.getString(16)));
            } else {
                //generate Edge
                e = new Edge(row.getString(17), new TypeValue(row.getString(0), row.getString(1)), new TypeValue(row.getString(2), row.getString(3)), row.getBoolean(4));
            }
            e.putProperty("byte", ((byte[]) row.get(5))[0]);
            e.putProperty("double", row.getDouble(6));
            e.putProperty("float", row.getFloat(7));
            e.putProperty("treeSet", new TreeSet<String>(row.getList(8)));
            e.putProperty("long", row.getLong(9));
            e.putProperty("short", ((Integer) row.getInt(10)).shortValue());
            e.putProperty("date", new Date(row.getLong(11)));
            e.putProperty("freqMap", new FreqMap(row.getJavaMap(12)));
            e.putProperty("count", row.getInt(13));
            e.putProperty("visibility", row.getString(14));
            elementList.add(e);
        }
        return elementList;
    }
}
