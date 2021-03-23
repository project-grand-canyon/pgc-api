package com.ccl.grandcanyon;

import com.ccl.grandcanyon.types.*;

import java.sql.*;
import java.util.logging.Logger;
import java.util.*;
import com.google.cloud.Tuple;

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

    private final static String SQL_INSERT_REMINDER_HISTORY = "INSERT into reminder_history ("
            + ReminderStatus.CALLER_ID + ", " + ReminderStatus.CALLER_DISTRICT_ID + ", "
            + ReminderStatus.TARGET_DISTRICT_ID + ", " + ReminderStatus.TIME_SENT + ", " + ReminderStatus.TRACKING_ID
            + ", " + ReminderStatus.EMAIL_DELIVERED + ", " + ReminderStatus.SMS_DELIVERED
            + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final static String SQL_STALE_SCRIPT_QUERY = "SELECT d.*, a.admin_id, a.login_enabled, a.email from districts d "
            + "LEFT JOIN admins_districts as ad ON ad.district_id = d.district_id "
            + "LEFT JOIN admins as a ON a.admin_id = ad.admin_id";

    private final static String SQL_UPDATE_STALE_SCRIPT_NOTIFICATION = "UPDATE districts SET "
            + District.LAST_STALE_SCRIPT_NOTIFICATION + " = ? " + "WHERE " + District.DISTRICT_ID + " = ?";

    private static final Logger logger = Logger.getLogger(ReminderSQLFetcher.class.getName());

    public void createInitialReminder(int callerId) throws SQLException {
        try (Connection conn = SQLHelper.getInstance().getConnection();
                PreparedStatement statement = conn.prepareStatement(SQL_INSERT_REMINDER);) {
            statement.setInt(1, callerId);
            statement.setInt(2, ReminderService.getNewDayOfMonth());
            statement.executeUpdate();
        } catch (Exception e) {

        }
    }

    public Caller getCallerById(int callerId) {
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
            return Callers.retrieveById(conn, callerId);
        } catch (Exception e) {
            logger.severe(String.format("Failed to fetch caller with id %d: %s", callerId, e.toString()));
            return null;
        }
    }

    public List<District> getDistricts() {
        String query = SQL_SELECT_REP_DISTRICTS;
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(query);
            List<District> districts = new ArrayList<District>();
            while (rs.next()){
                districts.add(new District(rs));
            }
            return districts;
        } catch (Exception e) {
            logger.severe("Failed to fetch districts: " + e.toString());
            return null;
        }
    }

    public List<CallerInfo> getCallerInfo(Set<ReminderDate> datesToQuery, District district) {
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
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(query);
            List<CallerInfo> info = new ArrayList<CallerInfo>();
            while(rs.next()) {
                info.add(new CallerInfo(new Caller(rs), new Reminder(rs)));
            }
            return info;
        } catch (Exception e) {
            logger.severe(String.format("Failed to fetch callers from district with id %d: %s", district.getDistrictId(), e.toString()));
            return null;
        }
    }

    public List<StaleScriptInfo> getStaleScriptInfo() {
        List<StaleScriptInfo> staleDistricts = new ArrayList<StaleScriptInfo>();
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(SQL_STALE_SCRIPT_QUERY);
            while(rs.next()) {
                staleDistricts.add(new StaleScriptInfo(new District(rs), rs.getBoolean(Admin.LOGIN_ENABLED), rs.getString(Admin.EMAIL)));
            }
        }
        catch (Exception e){
            logger.severe("Unexpected error checking for stale scripts: " + e.toString());
        }
        return staleDistricts;
    }

    public void updateStaleScript(District district) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
            PreparedStatement update = conn.prepareStatement(SQL_UPDATE_STALE_SCRIPT_NOTIFICATION);
            int idx = 1;
            update.setTimestamp(idx++, now);
            update.setInt(idx, district.getDistrictId());
            update.executeUpdate();
        } catch (Exception e) {
            logger.severe(String.format("Failed to update Stale Script for District with Id %d: %s", district.getDistrictId(), e.toString()));
        }
    }

    public District getCallerDistrict(Caller caller) {
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
            return  Districts.retrieveDistrictById(conn, caller.getDistrictId());
        } catch (Exception e) {
            logger.severe(String.format("Failed to fetch caller district for caller: %d", caller.getDistrictId()));
        }
        return null;
    }

    public Reminder getReminderByTrackingId(String trackingId) {
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
            PreparedStatement statement = conn.prepareStatement(SQL_SELECT_REMINDER);
            statement.setString(1, trackingId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new Reminder(rs);
            }
            return null;
        } catch (Exception e) {
            logger.severe("Failed to fetch reminder with tracking id " + trackingId + ": " + e.toString());
            return null;
        }
    }

    public DistrictHydrated getDistrictHydratedById(int id) {
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
            Districts districts = new Districts();
            return districts.retrieveDistrictHydratedById(conn, id);
        } catch (Exception e) {
            logger.severe(String.format("Failed to fetch Hydrated District: %d", id));
            return null;
        }
    }

    public District getDistrictById(int id) {
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
            return Districts.retrieveDistrictById(conn, id);
        } catch (Exception e) {
            logger.severe(String.format("Failed to fetch District: %d", id));
            return null;
        }
    }

    public void updateReminderStatus(ReminderStatus reminderStatus, ReminderDate reminderDate) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        try (Connection conn = SQLHelper.getInstance().getConnection()) {
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
            history.setInt(idx++, reminderStatus.getTargetDistrictId());
            history.setTimestamp(idx++, timestamp);
            history.setString(idx++, reminderStatus.getTrackingId());
            history.setBoolean(idx++, reminderStatus.getEmailDelivered());
            history.setBoolean(idx, reminderStatus.getSmsDelivered());
            history.executeUpdate();
        } catch(Exception e) {
            logger.severe(String.format("Failed to update reminder status for caller with id %d: %s", reminderStatus.getCaller().getCallerId(), e.toString()));
        }
    }
}