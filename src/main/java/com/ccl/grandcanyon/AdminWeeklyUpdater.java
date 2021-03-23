package com.ccl.grandcanyon;

import com.ccl.grandcanyon.deliverymethod.DeliveryService;
import com.ccl.grandcanyon.types.*;
import com.ccl.grandcanyon.utils.FileReader;
import com.google.cloud.Tuple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AdminWeeklyUpdater {

    private final static String EMAIL_DELIVERY_SERVICE = "emailDeliveryService";
    private final static String ADMIN_WEEKLY_UPDATER_INTERVAL = "adminWeeklyUpdaterInterval";

    private String weeklyReportHtml;
    private String weeklyReportEmailResource= "adminWeeklyReport.html";

    private final static String SQL_SELECT_ADMINS =
            "SELECT a.*, d.district_id FROM admins a " +
                    "LEFT JOIN admins_districts AS d ON a.admin_id = d.admin_id WHERE a." +
                    Admin.EMAIL + " IS NOT NULL AND a." +
                    Admin.LOGIN_ENABLED + " IS true AND a." +
                    Admin.MOST_RECENT_REPORT_SEND_TIME + " IS NULL OR a." +
                    Admin.MOST_RECENT_REPORT_SEND_TIME + " < DATE_SUB(NOW(), INTERVAL 1 WEEK)";

    private final static String SQL_SELECT_DISTRICTS =
            "SELECT * FROM districts";

    private final static String SQL_UPDATE_ADMIN_SEND_TIME =
            "UPDATE admins SET " +
                    Admin.MOST_RECENT_REPORT_SEND_TIME + " = NOW() " +
                    "WHERE " + Admin.ADMIN_ID + " = ?";

    private final static String SQL_SELECT_CALLS_FOR_DISTRICTS =
            "SELECT d.*, COUNT(c." + Call.CALLER_ID +
                    ") as calls, COUNT(DISTINCT(c." + Call.CALLER_ID +
                    ")) AS callers FROM calls c JOIN callers cr ON c." + Call.CALLER_ID + " = cr." + Caller.CALLER_ID +
                    " JOIN districts d ON d." + District.DISTRICT_ID + " = cr." + Caller.DISTRICT_ID +
                    " WHERE cr." + Caller.DISTRICT_ID +
                    " IN (?) AND c." + Call.CREATED +
                    " > DATE_SUB(NOW(), INTERVAL 1 WEEK) GROUP BY d." + District.DISTRICT_ID;

    private final static String SQL_SELECT_CALLER_COUNT =
            "SELECT d.*, COUNT(cr." + Caller.CALLER_ID +
                    ") AS caller_count FROM callers cr JOIN districts d ON d.district_id = cr.district_id WHERE cr." +
                    Caller.DISTRICT_ID + " IN (?) AND cr." +
                    Caller.PAUSED + " = false GROUP BY d." +
                    District.DISTRICT_ID;

    private final static String SQL_SELECT_NEW_CALLER_COUNT =
            "SELECT d.*, COUNT(cr." + Caller.CALLER_ID +
                    ") AS caller_count FROM callers cr JOIN districts d ON d.district_id = cr.district_id WHERE cr." +
                    Caller.DISTRICT_ID + " IN (?) AND cr." +
                    Caller.PAUSED + " = false AND cr." +
                    Caller.CREATED + " > DATE_SUB(NOW(), INTERVAL 1 WEEK) GROUP BY d." +
                    District.DISTRICT_ID;

    private static final Logger logger = Logger.getLogger(AdminWeeklyUpdater.class.getName());

    private static AdminWeeklyUpdater instance;

    // frequency, in minutes, that the service wakes up to send reminders
    private int serviceIntervalMinutes;

    private ScheduledFuture reporterTask;

    private DeliveryService emailDeliveryService;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static void init(Properties config) {
        assert(instance == null);
        instance = new AdminWeeklyUpdater(config);
    }

    public static AdminWeeklyUpdater getInstance() {
        assert(instance != null);
        return instance;
    }

    private AdminWeeklyUpdater(Properties config) {
        try {
            this.weeklyReportHtml = FileReader.create().read(weeklyReportEmailResource);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load welcome email template: " + e.getLocalizedMessage());
        }

        try {
            this.emailDeliveryService = (DeliveryService)Class.forName(
                    config.getProperty(EMAIL_DELIVERY_SERVICE)).getDeclaredConstructor().newInstance();
            this.emailDeliveryService.init(config);
        }
        catch (Exception e) {
            logger.warning("Failed to initialize Email deliver service: " + e.getMessage());
            this.emailDeliveryService = null;
        }

        this.serviceIntervalMinutes = Integer.parseInt(config.getProperty(ADMIN_WEEKLY_UPDATER_INTERVAL, "60"));
        this.reporterTask = executorService.scheduleAtFixedRate(new ReportSender(), 20, serviceIntervalMinutes * 60, TimeUnit.SECONDS);
    }

    public void tearDown() {
        if (reporterTask != null) {
            reporterTask.cancel(true);
        }
        executorService.shutdown();

        if(emailDeliveryService != null){
            emailDeliveryService.tearDown();
        }
    }

    public DeliveryService getEmailDeliveryService() {
        return emailDeliveryService;
    }

    class ReportSender implements Runnable {

        @Override
        public void run() {

            logger.info("Running admin weekly reporter");

            Connection conn = null;
            try {
                conn = SQLHelper.getInstance().getConnection();

                Map<Integer, District> districtMap = this.getDistricts(conn);
                Map<Integer, DistrictReport> reports = getReportInfo(conn, districtMap.keySet());
                List<Admin> adminsDueForReport = this.getAdminsDueForReport(conn);
                logger.info(adminsDueForReport.size() + " admins are due for a report");
                String dateStr = DateTimeFormatter.ofPattern("MM/dd/yyyy").format(LocalDate.now());

                for (Admin admin : adminsDueForReport) {
                    String messageStr = "";

                    Boolean hasReports = false;
                    for (Integer districtId: admin.getDistricts()) {
                        District d = districtMap.get(districtId);
                        if (d.isSenatorDistrict()) {
                            continue;
                        }
                        hasReports = true;
                        DistrictReport report = reports.get(districtId);
                        messageStr += d.readableName() + ": " +  report.calls + ", " + report.activeCallers + ", " + report.totalCallers + ", " + report.newCallers + "\n";
                    }

                    markReportSent(conn, admin.getAdminId());

                    if (hasReports) {
                        Message message = new Message();
                        message.setSubject("Monthly Calling Campaign Weekly Report " + dateStr);
                        message.setBody(weeklyReportHtml.replace("report_data", messageStr));
                        emailDeliveryService.sendTextMessage(admin, message);
                    }
                }
            } catch (Exception e) {
                logger.severe(e.getMessage());
            } finally {
                logger.info("Weekly report task done");
                if (conn != null) {
                    try {
                        conn.close();
                    }
                    catch (SQLException e) {
                        logger.warning("Failed to close SQL connection during reminder check: " + e.getMessage());
                    }
                }
            }
        }

        private void markReportSent(Connection conn, Integer adminId) throws SQLException {
            PreparedStatement statement = conn.prepareStatement(SQL_UPDATE_ADMIN_SEND_TIME);
            statement.setInt(1, adminId);
            statement.executeUpdate();
        }

        private Map<Integer, DistrictReport> getReportInfo(
                Connection conn, Set<Integer> districtIds
        ) throws SQLException {
            Map<Integer, Tuple<Integer, Integer>> districtCallCounts = this.getDistrictCallCounts(conn, districtIds);
            Map<Integer, Tuple<Integer, Integer>> districtCallerCounts = this.getDistrictCallerCounts(conn, districtIds);
            Map<Integer, DistrictReport> reports = new HashMap<Integer, DistrictReport>();
            for (Integer districtId: districtIds) {
                Tuple<Integer, Integer> calls = districtCallCounts.getOrDefault(districtId, Tuple.of(0, 0));
                Tuple<Integer, Integer> callers = districtCallerCounts.getOrDefault(districtId, Tuple.of(0, 0));
                reports.put(districtId, new DistrictReport(calls.x(), calls.y(), callers.x(), callers.y()));
            }
            return reports;
        }

        private Map<Integer, District> getDistricts(
                Connection conn) throws SQLException {
            PreparedStatement statement = conn.prepareStatement(SQL_SELECT_DISTRICTS);
            ResultSet rs = statement.executeQuery();
            Map<Integer, District> districts = new HashMap<Integer, District>();
            while(rs.next())  {
                District district = new District(rs);
                districts.put(district.getDistrictId(), district);
            }
            return districts;
        }

        private List<Admin> getAdminsDueForReport(
                Connection conn) throws SQLException {
            PreparedStatement statement = conn.prepareStatement(SQL_SELECT_ADMINS);
            ResultSet rs = statement.executeQuery();
            List<Admin> admins = new ArrayList<Admin>();
            while (rs.next()) {
                admins.add(new Admin(rs));
            }
            return admins;
        }

        private Map<Integer, Tuple<Integer, Integer>> getDistrictCallCounts (
                Connection conn, Set<Integer> districtIds) throws SQLException {
            String districts = districtIds.stream().map(Object::toString).collect(Collectors.joining(", "));
            PreparedStatement statement = conn.prepareStatement(SQL_SELECT_CALLS_FOR_DISTRICTS.replace("?", districts));
            ResultSet rs = statement.executeQuery();
            Map<Integer, Tuple<Integer, Integer>> districtCounts = new HashMap<Integer, Tuple<Integer, Integer>>();
            while (rs.next()) {

                Integer calls = rs.getInt("calls");
                Integer callers = rs.getInt("callers");
                District district = new District(rs);
                districtCounts.put(district.getDistrictId(), Tuple.of(calls, callers));
            }
            return districtCounts;
        }

        private Map<Integer, Tuple<Integer, Integer>> getDistrictCallerCounts (
                Connection conn, Set<Integer> districtIds) throws SQLException {
            String districts = districtIds.stream().map(Object::toString).collect(Collectors.joining(", "));
            PreparedStatement statement = conn.prepareStatement(SQL_SELECT_CALLER_COUNT.replace("?", districts));
            ResultSet rs = statement.executeQuery();

            Map<Integer, Integer> callerCounts = new HashMap<Integer, Integer>();
            while (rs.next()) {
                District district = new District(rs);
                Integer callers = rs.getInt("caller_count");
                callerCounts.put(district.getDistrictId(), callers);
            }

            PreparedStatement statement2 = conn.prepareStatement(SQL_SELECT_NEW_CALLER_COUNT.replace("?", districts));
            ResultSet rs2 = statement2.executeQuery();
            Map<Integer, Integer> newCallerCounts = new HashMap<Integer, Integer>();
            while (rs2.next()) {
                District district = new District(rs2);
                Integer callerz = rs2.getInt("caller_count");
                if (callerz == null) {
                    callerz = 0;
                }
                newCallerCounts.put(district.getDistrictId(), callerz);
            }

            Map<Integer, Tuple<Integer, Integer>> totalCounts =
                    callerCounts.entrySet().stream().collect(Collectors.toMap(
                            e -> e.getKey(),
                            e -> Tuple.of(e.getValue(), newCallerCounts.getOrDefault(e.getKey(), 0))
                    ));
            return totalCounts;
        }

    }

    class DistrictReport {

        private Integer calls;
        private Integer totalCallers;
        private Integer newCallers;
        private Integer activeCallers;

        public DistrictReport(Integer calls, Integer activeCallers, Integer totalCallers, Integer newCallers) {
            this.calls = calls;
            this.activeCallers = activeCallers;
            this.totalCallers = totalCallers;
            this.newCallers = newCallers;
        }


        public Integer getActiveCallers() {
            return activeCallers;
        }

        public void setActiveCallers(Integer activeCallers) {
            this.activeCallers = activeCallers;
        }

        public Integer getNewCallers() {
            return newCallers;
        }

        public void setNewCallers(Integer newCallers) {
            this.newCallers = newCallers;
        }

        public Integer getTotalCallers() {
            return totalCallers;
        }

        public void setTotalCallers(Integer totalCallers) {
            this.totalCallers = totalCallers;
        }

        public Integer getCalls() {
            return calls;
        }

        public void setCalls(Integer calls) {
            this.calls = calls;
        }
    }


}
