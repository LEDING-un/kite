package com.cloudera.data;

import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cloudera.data.filesystem.FileSystemMetadataProvider;
import com.cloudera.data.filesystem.FileSystemDatasetRepository;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public class TestPartitionedDatasetWriter {

  private Path testDirectory;
  private FileSystem fileSystem;
  private Schema testSchema;
  private FileSystemDatasetRepository repo;
  private PartitionedDatasetWriter<Object> writer;

  @Before
  public void setUp() throws IOException {
    testDirectory = new Path(Files.createTempDir().getAbsolutePath());
    fileSystem = FileSystem.get(new Configuration());
    testSchema = new Schema.Parser().parse(Resources.getResource(
        "schema/user-partitioned.avsc").openStream());
    repo = new FileSystemDatasetRepository(fileSystem, testDirectory,
        new FileSystemMetadataProvider(fileSystem, testDirectory));
    writer = new PartitionedDatasetWriter<Object>(repo.create(
        "users",
        new DatasetDescriptor.Builder()
            .schema(testSchema)
            .partitionStrategy(
                new PartitionExpression(testSchema
                    .getProp("cdk.partition.expression"), true).evaluate())
            .get()));
  }

  @After
  public void tearDown() throws IOException {
    fileSystem.delete(testDirectory, true);
  }

  @Test
  public void testBasicOpenClose() throws IOException {
    writer.open();
    writer.close();
  }

  @Test
  public void testWriter() throws IOException {
    Record record = new GenericRecordBuilder(testSchema).set("username",
        "test1").build();

    try {
      writer.open();
      writer.write(record);
      writer.flush();
      writer.close();
    } finally {
      Closeables.close(writer, true);
    }
  }

}
