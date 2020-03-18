package com.ccl.grandcanyon;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HolidayService {
    private final static String HOLIDAY_LIST_URL = "https://cclcalls.org/config/holidays.yaml";
    private final static Logger logger = Logger.getLogger(HolidayService.class.getName());
    private List<LocalDate> holidays;
    private static HolidayService instance;

    private HolidayService() {
        logger.info("Init HolidayService");
        loadHolidays();
    }

    public static void init(){
        assert(instance == null);
        instance = new HolidayService();
    }

    public void refresh(){
        loadHolidays();
    }

    public static HolidayService getInstance() {
        assert(instance != null);
        return instance;
    }

    public Boolean isHoliday(OffsetDateTime offsetDateTime) {
       return isHoliday(offsetDateTime.toLocalDate());
    }

    public Boolean isHoliday(LocalDate localDate) {
        return holidays.indexOf(localDate) != -1;
    }

    public List<LocalDate> getHolidays() {
        return holidays;
    }

    private void loadHolidays(){
        try {
            Yaml yaml = new Yaml();
            String holidaysYaml = getHolidaysYaml();
            List<Date> holidayDates = yaml.load(holidaysYaml);
            holidays = holidayDates.stream().map(it -> it.toInstant().atOffset(ZoneOffset.UTC).toLocalDate()).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load holiday yaml file: " + e.getLocalizedMessage());
        }
    }

    private String getHolidaysYaml() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HOLIDAY_LIST_URL))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
