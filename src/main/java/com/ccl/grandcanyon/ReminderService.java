package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.Caller;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ReminderService {

  private static ReminderService instance;
  private int serviceInternal;
  private int reminderInterval;
  private int secondReminderInterval;

  private ScheduledFuture reminderTask;

  public static void init(
      int serviceInterval,
      int reminderInterval,
      int secondReminderInterval) {

    assert(instance == null);
    instance = new ReminderService(serviceInterval, reminderInterval, secondReminderInterval);
  }

  public static ReminderService getInstance() {
    return instance;
  }


  private ReminderService(
      int serviceInterval,
      int reminderInterval,
      int secondReminderInterval) {

    this.reminderInterval = reminderInterval;
    this.secondReminderInterval = secondReminderInterval;
    this.reminderTask = Executors.newSingleThreadScheduledExecutor().
        scheduleAtFixedRate(new ReminderSender(), 60, serviceInterval*60, TimeUnit.SECONDS);
  }

  public void tearDown() {

    reminderTask.cancel(true);
  }


  class ReminderSender implements Runnable {

    @Override
    public void run() {

      try {
        List<Caller> callers = Callers.getAllCallers();
      }
      catch (SQLException e) {
        // log something here;
      }
      catch (Throwable e) {
        // all other errors
      }
    }
  }
}
