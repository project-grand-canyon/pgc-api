package com.ccl.grandcanyon;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URI;
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
        refresh();
    }

    public void refresh(){
        try {
            Yaml yaml = new Yaml();
            String holidaysYaml = getHolidaysYaml();
            List<Date> holidayDates = yaml.load(holidaysYaml);
            holidays = holidayDates.stream().map(it -> it.toInstant().atOffset(ZoneOffset.UTC).toLocalDate()).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load holiday yaml file: " + e.getLocalizedMessage());
        }
    }

    public static HolidayService getInstance() {
        if(instance == null) {
            instance = new HolidayService();
        }
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

    private String getHolidaysYaml() throws IOException, InterruptedException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(URI.create(HOLIDAY_LIST_URL));
        CloseableHttpResponse response = httpClient.execute(request);
        return EntityUtils.toString(response.getEntity());

    }
}
