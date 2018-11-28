package com.ccl.grandcanyon;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.util.Properties;


@WebListener
public class GCContextListener implements ServletContextListener {

  final static String sqlUrl = "sqlUrl";
  final static String sqlPoolSize = "sqlPoolSize";

  final static String createDistrictsTable =
      "CREATE TABLE IF NOT EXISTS districts ( " +
          "district_id INT NOT NULL AUTO_INCREMENT, " +
          "state VARCHAR(2) NOT NULL, " +
          "district_number INT NOT NULL, " +
          "representative VARCHAR(128), " +
          "info VARCHAR(512), " +
          "PRIMARY KEY (district_id) " +
          ")";

  final static String createCallersTable =
      "CREATE TABLE IF NOT EXISTS callers ( " +
          "caller_id INT NOT NULL AUTO_INCREMENT, " +
          "name VARCHAR(128) NOT NULL, " +
          "contact_method ENUM ('email', 'sms') NOT NULL, " +
          "phone VARCHAR(50), " +
          "email VARCHAR(100), " +
          "district_id INT NOT NULL, " +
          "zipcode VARCHAR(10), " +
          "UNIQUE (contact_method, phone), " +
          "UNIQUE (contact_method, email), " +
          "PRIMARY KEY (caller_id), " +
          "FOREIGN KEY (district_id) REFERENCES districts (district_id) " +
          ")";

  @Override
  public void contextInitialized(ServletContextEvent sce) {

    ServletContext servletContext = sce.getServletContext();
    Properties properties = new Properties();
    String url;
    try {
      properties.load(servletContext.getResourceAsStream(
          "/WEB-INF/classes/config.properties"));
    }
    catch (IOException e) {
      servletContext.log("Initialization failed: failed to load config.properties", e);
      return;
    }

    if (!(properties.containsKey(sqlPoolSize)) || !properties.containsKey(sqlUrl)) {
      servletContext.log(
          "Initialization failed: config.properties missing one or more required properties");
      return;
    }
    url = properties.getProperty(sqlUrl);
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setMaximumPoolSize(Integer.parseInt(properties.getProperty(sqlPoolSize)));
    SQLHelper.init(new HikariDataSource(config));

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        SQLHelper sqlHelper = SQLHelper.getInstance();
        if (sqlHelper != null) {
          sqlHelper.tearDown();
        }
      }

    });
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    SQLHelper sqlHelper = SQLHelper.getInstance();
    if (sqlHelper != null) {
      sqlHelper.tearDown();
    }

  }


}
