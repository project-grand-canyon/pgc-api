package com.ccl.grandcanyon;

import com.ccl.grandcanyon.deliverymethod.DeliveryService;
import com.ccl.grandcanyon.types.Admin;
import com.ccl.grandcanyon.types.Admin.IncludeDistricts;
import com.ccl.grandcanyon.types.District;
import com.ccl.grandcanyon.types.Message;
import com.ccl.grandcanyon.types.Status;
import com.ccl.grandcanyon.utils.FileReader;
import com.ccl.grandcanyon.utils.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.util.Pair;

public class DistrictStatusReminderService {
    private static final Logger logger = Logger.getLogger(DistrictStatusReminderService.class.getName());
    private static DistrictStatusReminderService instance;
    private DeliveryService emailDeliveryService;
    private static final String reminderEmailResource = "adminStatusReminderEmail.html";
    private String reminderHtmlBody;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> reminderTask;

    private static final String SQL_SELECT_ADMINS =
            "SELECT a.*, d.* FROM admins a " +
                    "JOIN admins_districts AS ad ON a.admin_id = ad.admin_id " +
                    "JOIN districts AS d ON ad.district_id = d.district_id " +
                    "WHERE d.status != 'active' " +
                    "ORDER BY a.admin_id";

    private DistrictStatusReminderService() {
        ReminderService reminderService = ReminderService.getInstance();
        emailDeliveryService = reminderService.getEmailDeliveryService();

        try {
            this.reminderHtmlBody = new FileReader().read(reminderEmailResource);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load admin status reminder email template: " + e.getLocalizedMessage());
        }

        this.reminderTask = createReminderTask();
    }

    public static void init() {
        assert (instance == null);
        instance = new DistrictStatusReminderService();
    }

    public void tearDown() {
        reminderTask.cancel(true);
        executorService.shutdown();
    }

    public static DistrictStatusReminderService getInstance() {
        assert (instance != null);
        return instance;
    }

    private void sendReminderEmails() {
        List<Pair<Admin, List<District>>> adminsAndDistricts = getAdminsAndDistricts();
        for (Pair<Admin, List<District>> pair : adminsAndDistricts) {
            sendReminderEmails(pair.getKey(), pair.getValue());
        }
    }

    private void sendReminderEmails(Admin admin, List<District> districts) {
        List<District> covidPausedDistricts = districts.stream()
                .filter(district -> district.getStatus() == Status.covid_paused)
                .collect(Collectors.toList());
        if (!covidPausedDistricts.isEmpty()) {
            sendCovidPausedReminderEmail(admin, covidPausedDistricts);
        }
    }

    private void sendCovidPausedReminderEmail(Admin admin, List<District> districts) {
        assert (!districts.isEmpty());
        final String districtsPhrase = createDistrictsPhrase(districts);
        final String messageSubject = "Auto-reminder: Time to re-enable MCC?";
        final String messageBody = this.reminderHtmlBody
                .replaceAll("\\{districts_phrase}", districtsPhrase);
        Message message = new Message();
        message.setSubject(messageSubject);
        message.setBody(messageBody);
        try {
            boolean messageSent = emailDeliveryService.sendTextMessage(admin, message);
            if (messageSent) {
                logger.info(String.format("Sent covid pause reminder email to admin {id: %d}", admin.getAdminId()));
            }
        } catch (Exception e) {
            logger.severe(String.format("Failed to send covid pause reminder email to admin {id: %d}", admin.getAdminId()));
        }
    }


    private String createDistrictsPhrase(List<District> districts){
        assert (!districts.isEmpty());
        final String firstPart = districts.size() > 1 ? "districts " : "district ";
        final String lastPart = districts.size() > 1 ? " have their " : " has its ";
        List<String> districtNames = districts.stream()
                .map(District::toString)
                .collect(Collectors.toList());
        final String districtNamesCommaSeparated = StringUtils.createCommaSeparatedList(districtNames);
        return firstPart + districtNamesCommaSeparated + lastPart;
    }

    // Returns pairs of admins and their associated districts which don't have an active status
    private List<Pair<Admin, List<District>>> getAdminsAndDistricts() {
        List<Pair<Admin, List<District>>> result = new ArrayList<>();
        Connection conn = null;
        try {
            conn = SQLHelper.getInstance().getConnection();
            ResultSet rs = conn.createStatement().executeQuery(SQL_SELECT_ADMINS);
            while (rs.next()) {
                Admin admin = new Admin(rs, IncludeDistricts.NO);
                if (result.isEmpty() || admin.getAdminId() != result.get(result.size() - 1).getKey().getAdminId()) {
                    result.add(new Pair<>(admin, new ArrayList<>()));
                }
                District district = new District(rs);
                result.get(result.size() - 1).getValue().add(district);
            }

            return result;
        } catch (Exception e) {
            logger.severe("Failed to get admins and associated districts: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.warning("Failed to close SQL connection: " + e.getMessage());
                }
            }
        }
    }


    private ScheduledFuture<?> createReminderTask() {
        final int REMINDER_HOUR = 20;
        final DayOfWeek REMINDER_DAY = DayOfWeek.SUNDAY;

        OffsetDateTime currentDateTime = OffsetDateTime.now(ZoneId.of("EST"));
        OffsetDateTime nextRunDateTime = currentDateTime;
        while (nextRunDateTime.getDayOfWeek() != REMINDER_DAY) {
            nextRunDateTime = nextRunDateTime.plusDays(1);
        }
        nextRunDateTime = nextRunDateTime.withHour(REMINDER_HOUR).truncatedTo(ChronoUnit.HOURS);

        if (nextRunDateTime.toLocalDate().equals(currentDateTime.toLocalDate()) && currentDateTime.getHour() >= REMINDER_HOUR) {
            nextRunDateTime = nextRunDateTime.plusWeeks(1);
        }

        long initialDelay = Duration.between(currentDateTime, nextRunDateTime).toSeconds();
        return executorService.scheduleAtFixedRate(new ReminderSender(), initialDelay, TimeUnit.DAYS.toSeconds(7), TimeUnit.SECONDS);
    }

    private class ReminderSender implements Runnable {
        @Override
        public void run() {
            logger.info("Running district status reminder sender");
            sendReminderEmails();
        }
    }
}
