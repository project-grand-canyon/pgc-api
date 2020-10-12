package com.ccl.grandcanyon;


import com.ccl.grandcanyon.types.*;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.sql.*;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Path("stats")
public class Stats {

  private static final String SQL_SELECT_CALLS = "SELECT * FROM calls";

  private static final String SQL_COUNT_CALLERS =
      "SELECT COUNT(caller_id) as numCallers, MONTH(created) as month, YEAR(created) AS year FROM callers where paused = false";

  private static final String SQL_COUNT_ACTIVE_CALLERS_TOTAL =
      "SELECT count(*) as numCallers FROM " +
              "(SELECT MAX(c.created) call_time, c.caller_id FROM calls c GROUP BY c.caller_id) lc";

  private static final String SQL_COUNT_ACTIVE_CALLERS_DISTRICT =
          "SELECT count(*) as numCallers FROM " +
                  "(SELECT MAX(c.created) call_time, c.caller_id FROM calls c JOIN callers clr on c.caller_id = clr.caller_id WHERE clr.district_id = ? GROUP BY c.caller_id) lc";

  private static final String SQL_COUNT_ACTIVE_CALLERS_BY_MONTH =
      "SELECT COUNT(c.caller_id) as numCallers, c.month, c.year FROM calls c JOIN callers clr ON c.caller_id = clr.caller_id";

  private static final String SQL_COUNT_REMINDERS =
      "SELECT COUNT(*) as numReminders, MONTH(time_sent) as month, YEAR(time_sent) AS year FROM reminder_history";

  private static final String GROUP_BY_MONTH =
      " GROUP BY " + Call.MONTH + ", " + Call.YEAR;

  private static final String GROUP_BY_MONTH_ACTIVE_CALLERS =
      " GROUP BY c." + Call.MONTH + ", c." + Call.YEAR;

  // configuration property name
  final static String RECENT_DAY_COUNT = "recentDayCount";
  private static int recentDayCount;
  private static long recencyInterval;
  private static String whereClauseRecency;

  @Context
  UriInfo uriInfo;

  @Context
  ContainerRequestContext requestContext;



  static void init(Properties properties) {
    String recentDayCountStr = properties.getProperty(RECENT_DAY_COUNT, "30");
    recentDayCount = Integer.parseInt(recentDayCountStr);
    recencyInterval = TimeUnit.DAYS.toMillis(recentDayCount);
    whereClauseRecency = " WHERE call_time > DATE_SUB(NOW(), INTERVAL " + recentDayCountStr + " DAY);";
  }


  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response getStats() throws SQLException {

    boolean hasPrivilege = requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL) != null;
    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_CALLS);
      List<Call> calls = new ArrayList<>();
      while (rs.next()) {
        calls.add(new Call(rs));
      }
      rs.close();

      GlobalStats stats = new GlobalStats();
      getCallStats(calls, stats);

      ResultSet rs2 = conn.createStatement().executeQuery(SQL_COUNT_CALLERS + GROUP_BY_MONTH);
      getCallerStats(rs2, stats);
      rs2.close();

      if (hasPrivilege) {
        ResultSet rs3 = conn.createStatement().executeQuery(SQL_COUNT_ACTIVE_CALLERS_BY_MONTH + GROUP_BY_MONTH_ACTIVE_CALLERS);
        getActiveCallersByMonth(rs3, stats);
        rs3.close();

        ResultSet rs4 = conn.createStatement().executeQuery(SQL_COUNT_REMINDERS + GROUP_BY_MONTH);
        getReminderStats(rs4, stats);
        rs4.close();

        PreparedStatement recentActiveCallersStatement = conn.prepareStatement(SQL_COUNT_ACTIVE_CALLERS_TOTAL + whereClauseRecency);
        ResultSet rs5 = recentActiveCallersStatement.executeQuery();
        if (rs5.next()) {
          stats.setTotalRecentActiveCallers(rs5.getInt("numCallers"));
        }
        rs5.close();
      }
      return Response.ok(stats).build();

    }
    finally {
      conn.close();
    }
  }


  @GET
  @Path("{districtId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(GCAuth.ANONYMOUS)
  public Response getDistrictStats(
      @PathParam("districtId") int districtId)
      throws SQLException {

    boolean hasPrivilege = requestContext.getProperty(GCAuth.CURRENT_PRINCIPAL) != null;
    Connection conn = SQLHelper.getInstance().getConnection();
    try {
      String whereClauseCalls = " WHERE " + Call.DISTRICT_ID + " = ?";
      PreparedStatement callStatement = conn.prepareStatement(SQL_SELECT_CALLS + whereClauseCalls);
      callStatement.setInt(1, districtId);

      ResultSet rs = callStatement.executeQuery();

      List<Call> calls = new ArrayList<>();
      while (rs.next()) {
        calls.add(new Call(rs));
      }

      DistrictStats stats = new DistrictStats();
      stats.setDistrictId(districtId);
      getCallStats(calls, stats);

      PreparedStatement callerStatement = conn.prepareStatement(
          SQL_COUNT_CALLERS + " AND " + Caller.DISTRICT_ID + " = ?" + GROUP_BY_MONTH);
      callerStatement.setInt(1, districtId);
      ResultSet rs2 = callerStatement.executeQuery();
      getCallerStats(rs2, stats);
      rs2.close();

      if (hasPrivilege) {
        String whereClauseCallers = " WHERE clr." + Caller.DISTRICT_ID + " = ?";
        PreparedStatement activeCallerStatement = conn.prepareStatement(SQL_COUNT_ACTIVE_CALLERS_BY_MONTH +
            whereClauseCallers + GROUP_BY_MONTH_ACTIVE_CALLERS);
        activeCallerStatement.setInt(1, districtId);
        ResultSet rs3 = activeCallerStatement.executeQuery();
        getActiveCallersByMonth(rs3, stats);
        rs3.close();

        String reminderWhereClause = " WHERE " + ReminderStatus.CALLER_DISTRICT_ID + " = ?";
        PreparedStatement reminderStatement = conn.prepareStatement(SQL_COUNT_REMINDERS +
            reminderWhereClause + GROUP_BY_MONTH);
        reminderStatement.setInt(1, districtId);
        ResultSet rs4 = reminderStatement.executeQuery();
        getReminderStats(rs4, stats);
        rs4.close();

        PreparedStatement recentActiveCallersStatement = conn.prepareStatement(SQL_COUNT_ACTIVE_CALLERS_DISTRICT + whereClauseRecency);
        recentActiveCallersStatement.setInt(1, districtId);
        ResultSet rs5 = recentActiveCallersStatement.executeQuery();
        if (rs5.next()) {
          stats.setTotalRecentActiveCallers(rs5.getInt("numCallers"));
        }
        rs5.close();
      }
      return Response.ok(stats).build();
    }
    finally {
      conn.close();
    }
  }



  private void getCallStats(
      List<Call> calls,
      GlobalStats stats) {
    SortedMap<YearMonth, Integer> callsByMonth = new TreeMap<>();
    int totalRecentCalls = 0;

    Timestamp isRecentTime = new Timestamp(System.currentTimeMillis() - recencyInterval);

    for (Call call : calls) {
      // this increments the value if it exists, otherwise sets it to 1
      YearMonth yearMonth = YearMonth.of(call.getYear(), call.getMonth());
      callsByMonth.merge(yearMonth, 1, (prev, v) -> prev+v);

      if (call.getCreated().after(isRecentTime)) {
        totalRecentCalls++;
      }
    }

    stats.setTotalCalls(calls.size());
    stats.setCallsByMonth(callsByMonth);
    stats.setTotalRecentCalls(totalRecentCalls);
    stats.setRecentDayCount(recentDayCount);
  }



  private void getCallerStats(
      ResultSet rs,
      GlobalStats stats) throws SQLException {

    int totalCallers = 0;
    SortedMap<YearMonth, Integer> callersCreatedByMonth = new TreeMap<>();

    while (rs.next()) {

      int numCallers = rs.getInt("numCallers");
      totalCallers += numCallers;

      YearMonth ym = YearMonth.of(rs.getInt(Call.YEAR), rs.getInt(Call.MONTH));
      callersCreatedByMonth.put(ym, numCallers);
    }

    // build accumlated caller count from earliest caller to current.  Both maps
    // are sorted from earliest month to current month.
    SortedMap<YearMonth, Integer> accumulatedCallers = new TreeMap<>();
    YearMonth thisMonth = YearMonth.now();
    for (Map.Entry<YearMonth, Integer> entry : callersCreatedByMonth.entrySet()) {
      YearMonth createdMonth = entry.getKey();
      Integer count = entry.getValue();
      for (YearMonth ym = createdMonth; !ym.isAfter(thisMonth); ym = ym.plusMonths(1)) {
        accumulatedCallers.merge(ym, count, (prev, v) -> prev + v);
      }
    }

    stats.setTotalCallers(totalCallers);
    stats.setCallersByMonth(accumulatedCallers);
  }


  private void getActiveCallersByMonth(
      ResultSet rs,
      GlobalStats stats) throws SQLException {

    SortedMap<YearMonth, Integer> activeCallersByMonth = new TreeMap<>();
    while (rs.next()) {
      YearMonth ym = YearMonth.of(rs.getInt(Call.YEAR), rs.getInt(Call.MONTH));
      activeCallersByMonth.put(ym, rs.getInt("numCallers"));
    }
    YearMonth thisMonth = YearMonth.now();
    if (!stats.getCallersByMonth().isEmpty()) {
      for (YearMonth ym = stats.getCallersByMonth().firstKey(); !ym.isAfter(thisMonth); ym = ym.plusMonths(1)) {
        activeCallersByMonth.putIfAbsent(ym, 0);
      }
    }
    stats.setActiveCallersByMonth(activeCallersByMonth);
  }

  private void getReminderStats(
      ResultSet rs,
      GlobalStats stats) throws SQLException {

    SortedMap<YearMonth, Integer> remindersByMonth = new TreeMap<>();
    while (rs.next()) {
      YearMonth ym = YearMonth.of(rs.getInt(Call.YEAR), rs.getInt(Call.MONTH));
      remindersByMonth.put(ym, rs.getInt("numReminders"));
    }
    stats.setRemindersByMonth(remindersByMonth);
  }

}
