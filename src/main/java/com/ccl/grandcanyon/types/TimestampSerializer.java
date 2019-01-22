package com.ccl.grandcanyon.types;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * JSON serializer for SQL timestamps.
 */
public class TimestampSerializer extends JsonSerializer<Timestamp> {

  private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  @Override
  public void serialize(
      Timestamp timestamp,
      JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) throws IOException {

    jsonGenerator.writeString(formatter.format(timestamp.toInstant().atZone(
        ZoneId.systemDefault())));

  }
}
