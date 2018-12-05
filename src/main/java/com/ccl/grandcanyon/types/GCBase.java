package com.ccl.grandcanyon.types;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Base class for data model types.
 */
public abstract class GCBase {

  private static final String CREATED = "created";
  private static final String LAST_MODIFIED = "last_modified";

  private Timestamp created;
  private Timestamp lastModified;

  GCBase(ResultSet rs) throws SQLException {
    this.created = rs.getTimestamp(CREATED);
    this.lastModified = rs.getTimestamp(LAST_MODIFIED);
  }

  GCBase() {}


  @JsonSerialize(using = TimestampSerializer.class)
  public Timestamp getCreated() {
    return created;
  }

  @JsonSerialize(using = TimestampSerializer.class)
  public Timestamp getLastModified() {
    return lastModified;
  }

}
