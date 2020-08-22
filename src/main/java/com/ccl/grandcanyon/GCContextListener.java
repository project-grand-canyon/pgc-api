package com.ccl.grandcanyon;

import com.ccl.grandcanyon.auth.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;


@WebListener
public class GCContextListener implements ServletContextListener {

  // configuration property names
  final static String sqlUrl = "sqlUrl";
  final static String sqlPoolSize = "sqlPoolSize";

  final static String jwtLifetime = "jwtLifetime";
  final static String jwtRefreshInterval = "jwtRefreshInterval";

  private static final Logger logger = Logger.getLogger(GCContextListener.class.getName());

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

    try {
      ReminderService.init(properties);
    }
    catch (Exception e) {
      throw new RuntimeException(
          "Initialization failed: error initializing Reminder service", e);
    }

    try {
      WelcomeService.init(properties);
    }
    catch (Exception e) {
      throw new RuntimeException(
              "Initialization failed: error initializing Welcome service", e);
    }

    try {
      AdminWelcomeService.init(properties);
    }
    catch (Exception e) {
      throw new RuntimeException(
              "Initialization failed: error initializing Admin Welcome service", e);
    }

    try {
      EventAlertingService.init(properties);
    }
    catch (Exception e) {
      throw new RuntimeException(
              "Initialization failed: error initializing EventAlertingService", e);
    }

    try {
      AuthenticationService.init(
          Integer.parseInt(properties.getProperty(jwtLifetime, "240")),
          Integer.parseInt(properties.getProperty(jwtRefreshInterval, "60")));
    }
    catch (JOSEException e) {
      throw new RuntimeException(
          "Initialization failed: failed to initialize Authentication Service", e);
    }

    Stats.init(properties);

    Admins.init(properties);

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

    try {
      AuthenticationService.getInstance().tearDown();
    } catch (Exception e) {
      logger.warning("Failed to teardown AuthenticationService: " + e.getMessage());
    }

    try {
      EventAlertingService.getInstance().tearDown();
    } catch (Exception e) {
      logger.warning("Failed to teardown EventAlertingService: " + e.getMessage());
    }

    try {
      AdminWelcomeService.getInstance().tearDown();
    } catch (Exception e) {
      logger.warning("Failed to teardown AdminWelcomeService: " + e.getMessage());
    }

    try {
      WelcomeService.getInstance().tearDown();
    } catch (Exception e) {
      logger.warning("Failed to teardown WelcomeService: " + e.getMessage());
    }

    try {
      ReminderService.getInstance().tearDown();
    } catch (Exception e) {
      logger.warning("Failed to teardown ReminderService: " + e.getMessage());
    }

    DistrictStatusChangeService.getInstance().tearDown();
  }
}
