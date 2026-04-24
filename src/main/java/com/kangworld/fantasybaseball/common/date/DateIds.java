package com.kangworld.fantasybaseball.common.date;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateIds {

    private static final DateTimeFormatter DATE_ID_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    public static int from(LocalDate date) {
        return Integer.parseInt(date.format(DATE_ID_FORMATTER));
    }

    public static LocalDate toDate(int dateId) {
        return LocalDate.parse(String.valueOf(dateId), DATE_ID_FORMATTER);
    }

}
