package com.ccl.grandcanyon;

import com.ccl.grandcanyon.auth.AuthenticationService;
import com.nimbusds.jose.JOSEException;
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

  // configuration property names
  final static String sqlUrl = "sqlUrl";
  final static String sqlPoolSize = "sqlPoolSize";

  final static String reminderServiceInterval = "reminderServiceInterval";
  final static String reminderInterval = "reminderInterval";
  final static String secondReminderInterval = "secondReminderInterval";

  final static String jwtLifetime = "jwtLifetime";
  final static String jwtRefreshInterval = "jwtRefreshInterval";


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
      throw new RuntimeException(
          "Initialization failed: failed to load config.properties", e);
    }

    if (!properties.containsKey(sqlUrl)) {
      throw new RuntimeException(
          "Initialization failed: config.properties missing one or more required properties");
    }
    url = properties.getProperty(sqlUrl);
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setMaximumPoolSize(Integer.parseInt(properties.getProperty(sqlPoolSize, "3")));
    SQLHelper.init(new HikariDataSource(config));

    ReminderService.init(
        Integer.parseInt(properties.getProperty(reminderServiceInterval, "60")),
        Integer.parseInt(properties.getProperty(reminderInterval, "30")),
        Integer.parseInt(properties.getProperty(secondReminderInterval, "4")));

    try {
      AuthenticationService.init(
          Integer.parseInt(properties.getProperty(jwtLifetime, "240")),
          Integer.parseInt(properties.getProperty(jwtRefreshInterval, "60")));
    }
    catch (JOSEException e) {
      throw new RuntimeException(
          "Initialization failed: failed to initialize Authentication Service", e);
    }

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

    ReminderService reminderService = ReminderService.getInstance();
    if (reminderService != null) {
      reminderService.tearDown();
    }

    AuthenticationService authService = AuthenticationService.getInstance();
    if (authService != null) {
      authService.tearDown();
    }

  }


}
