package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;

import java.sql.*;
import java.util.logging.Logger;
import java.util.*;


public class ReminderSQLFetcher {
    private final static String SQL_SELECT_REMINDER = "SELECT * FROM reminders WHERE " + Reminder.TRACKING_ID + " = ?";

    private final static String SQL_INSERT_REMINDER = "INSERT into reminders (" + Reminder.CALLER_ID + ", "
        + Reminder.DAY_OF_MONTH + ") VALUES (?, ?)";

    private final static String SQL_UPDATE_REMINDER = "UPDATE reminders SET " + Reminder.LAST_REMINDER_TIMESTAMP
        + " = ?, " + Reminder.TRACKING_ID + " = ?, " + Reminder.REMINDER_YEAR + " = ?, " + Reminder.REMINDER_MONTH
        + " = ? " + "WHERE " + Reminder.CALLER_ID + " = ?";

    private final static String SQL_SELECT_REP_DISTRICTS = "SELECT * FROM districts WHERE district_number >= 0";

    private final static String SQL_SELECT_CALLERS = "SELECT r.*, c.*, ccm.contact_method, last_call_timestamp FROM reminders r "
        + "LEFT JOIN callers AS c ON c.caller_id = r.caller_id "
        + "LEFT JOIN callers_contact_methods AS ccm on c.caller_id = ccm.caller_id "
        + "LEFT JOIN (SELECT caller_id,  MAX(created) as last_call_timestamp FROM calls GROUP by caller_id) cls ON c.caller_id = cls.caller_id";

    private final static String SQL_INSERT_REMINDER_HISTORY = "INSERT into reminder_history (" + ReminderStatus.CALLER_ID
        + ", " + ReminderStatus.CALLER_DISTRICT_ID + ", " + ReminderStatus.TARGET_DISTRICT_ID + ", "
        + ReminderStatus.TIME_SENT + ", " + ReminderStatus.TRACKING_ID + ", " + ReminderStatus.EMAIL_DELIVERED + ", "
        + ReminderStatus.SMS_DELIVERED + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final static String SQL_STALE_SCRIPT_QUERY = "SELECT d.*, a.admin_id, a.login_enabled, a.email from districts d "
        + "LEFT JOIN admins_districts as ad ON ad.district_id = d.district_id "
        + "LEFT JOIN admins as a ON a.admin_id = ad.admin_id";

    private final static String SQL_UPDATE_STALE_SCRIPT_NOTIFICATION = "UPDATE districts SET "
        + District.LAST_STALE_SCRIPT_NOTIFICATION + " = ? " + "WHERE " + District.DISTRICT_ID + " = ?";

    private static final Logger logger = Logger.getLogger(ReminderSQLFetcher.class.getName());
    
    private static ReminderSQLFetcher instance;

    private static Connection conn; 

    public static void init(Properties config) {
        assert (instance == null);
        instance = new ReminderSQLFetcher(config);
    }
    
    public static ReminderSQLFetcher getInstance() {
        assert (instance != null);
        return instance;
    }
    
    private ReminderSQLFetcher(Properties config) {
        logger.info("Init Reminder SQL Fetcher");
    }

    public static void openConnection() {
        try {
            conn = SQLHelper.getInstance().getConnection();
        } catch (Throwable e) {
            logger.severe("Unable to open connection: " + e.toString());
        }
    }

    public static void closeConnection() {
        if (conn != null){
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warning("Failed to close SQL connection during reminder check: " + e.getMessage());
            }
        }
    }

    public static void createInitialReminder(Connection conn, int callerId) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(SQL_INSERT_REMINDER);
        statement.setInt(1, callerId);
        statement.setInt(2, ReminderService.getNewDayOfMonth());
        statement.executeUpdate();
    }

    public static Caller getCallerById(int callerId) throws SQLException {
        return Callers.retrieveById(conn, callerId);        
    }

    public static ResultSet getDistrictSet() throws SQLException {
        String query = SQL_SELECT_REP_DISTRICTS;
        logger.info(query);
        return conn.createStatement().executeQuery(query);
    } 

    public static ResultSet getCallerSet(Set<ReminderDate> datesToQuery, District district) throws SQLException {
        StringBuilder whereClause = new StringBuilder(" WHERE " + Reminder.DAY_OF_MONTH);
        if (datesToQuery.size() == 1) {
            whereClause.append(" = ").append(datesToQuery.iterator().next().getDay());
        } else {
            whereClause.append(" IN (");
            for (ReminderDate date : datesToQuery) {
                whereClause.append(date.getDay()).append(",");
            }
            whereClause.deleteCharAt(whereClause.length() - 1);
            whereClause.append(")");
        }
  
        whereClause.append(" AND c." + Caller.DISTRICT_ID + " = " + district.getDistrictId());
  
        String query = SQL_SELECT_CALLERS + whereClause.toString();
        logger.info(query);
        return conn.createStatement().executeQuery(query);
    }

    public static ResultSet getStaleScriptSet() throws SQLException {
        return conn.createStatement().executeQuery(SQL_STALE_SCRIPT_QUERY);
    }

    public static void updateStaleScript(District district) throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        PreparedStatement update = conn.prepareStatement(SQL_UPDATE_STALE_SCRIPT_NOTIFICATION);
        int idx = 1;
        update.setTimestamp(idx++, now);
        update.setInt(idx, district.getDistrictId());
        update.executeUpdate();
    }

    public static District getCallerDistrict(Caller caller) throws SQLException {
        return Districts.retrieveDistrictById(conn, caller.getDistrictId());
    }

    public static Reminder getReminderByTrackingId(String trackingId) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(SQL_SELECT_REMINDER);
        statement.setString(1, trackingId);
        ResultSet rs = statement.executeQuery();
        if (rs.next()) {
            return new Reminder(rs);
        }
        return null;
    }

    public static DistrictHydrated getDistrictHydratedById(int id) {
        try{
            return Districts.retrieveDistrictHydratedById(conn, id);
        }catch(Exception e) {
            logger.severe(String.format("Failed to fetch Hydrated District: %d", id));
            return null;
        }
    }

    public static District getDistrictById(int id) {
        try{
            return Districts.retrieveDistrictById(conn, id);
        } catch(Exception e) {
            logger.severe(String.format("Failed to fetch Hydrated District: %d", id));
            return null;
        }
    }

    public static void updateReminderStatus(ReminderStatus reminderStatus, ReminderDate reminderDate) throws SQLException
         {

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    PreparedStatement update = conn.prepareStatement(SQL_UPDATE_REMINDER);
    int idx = 1;
    update.setTimestamp(idx++, timestamp);
    update.setString(idx++, reminderStatus.getTrackingId());
    update.setInt(idx++, reminderDate.getYear());
    update.setInt(idx++, reminderDate.getMonth());
    update.setInt(idx, reminderStatus.getCaller().getCallerId());
    update.executeUpdate();

    // add history record
    PreparedStatement history = conn.prepareStatement(SQL_INSERT_REMINDER_HISTORY);
    idx = 1;
    history.setInt(idx++, reminderStatus.getCaller().getCallerId());
    history.setInt(idx++, reminderStatus.getCaller().getDistrictId());
    history.setInt(idx++, reminderStatus.getTargetDistrict().getDistrictId());
    history.setTimestamp(idx++, timestamp);
    history.setString(idx++, reminderStatus.getTrackingId());
    history.setBoolean(idx++, reminderStatus.getEmailDelivered());
    history.setBoolean(idx, reminderStatus.getSmsDelivered());
    history.executeUpdate();
    }
}