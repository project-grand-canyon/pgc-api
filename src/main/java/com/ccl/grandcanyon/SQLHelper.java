package com.ccl.grandcanyon;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLHelper {

  private static SQLHelper instance;

  private HikariDataSource dataSource;

  public static void init(HikariDataSource dataSource) {
    assert(instance == null);
    instance = new SQLHelper(dataSource);
  }

  public static SQLHelper getInstance() {
    return instance;
  }

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public void tearDown() {
    dataSource.close();
  }

  SQLHelper(HikariDataSource dataSource) {
    this.dataSource = dataSource;
  }
}
