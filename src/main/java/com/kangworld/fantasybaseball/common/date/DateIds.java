package com.kangworld.fantasybaseball.common.date;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateIds {

    private static final DateTimeFormatter DATE_ID_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private DateIds() {
    }

    public static int from(LocalDate date) {
        return Integer.parseInt(date.format(DATE_ID_FORMATTER));
    }

}
