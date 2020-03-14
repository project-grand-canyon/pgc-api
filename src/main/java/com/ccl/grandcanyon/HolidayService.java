package com.ccl.grandcanyon;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HolidayService {
    private final static String HOLIDAY_LIST_FILEPATH = "./config/holidays.yaml";
    private final static Logger logger = Logger.getLogger(HolidayService.class.getName());
    private final List<LocalDate> holidays;
    private static HolidayService instance;

    // Allowed to call multiple times to update `holidays`
    public static void init(){
        instance = new HolidayService();
    }

    public static HolidayService getInstance() {
        assert(instance != null);
        return instance;
    }

    private HolidayService() {
        logger.info("Init HolidayService");
        try {
            Yaml yaml = new Yaml();
            InputStream stream = new FileInputStream(HOLIDAY_LIST_FILEPATH);
            List<Date> holidayDates = yaml.load(stream);
            holidays = holidayDates.stream().map(it -> it.toInstant().atOffset(ZoneOffset.UTC).toLocalDate()).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load holiday yaml file: " + e.getLocalizedMessage());
        }
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
}
