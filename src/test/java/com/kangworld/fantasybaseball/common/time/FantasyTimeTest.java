package com.kangworld.fantasybaseball.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class FantasyTimeTest {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Test
    void now는_03시_전이면_전날을_반환한다() {
        FantasyTime fantasyTime = new FantasyTime(fixedClock("2026-05-08T17:59:00Z"));

        LocalDate currentDate = fantasyTime.now();

        assertThat(currentDate).isEqualTo(LocalDate.of(2026, 5, 8));
    }

    @Test
    void now는_03시부터_당일을_반환한다() {
        FantasyTime fantasyTime = new FantasyTime(fixedClock("2026-05-08T18:00:00Z"));

        LocalDate currentDate = fantasyTime.now();

        assertThat(currentDate).isEqualTo(LocalDate.of(2026, 5, 9));
    }

    @Test
    void getDateTime은_주입된_clock의_현재_시각을_반환한다() {
        FantasyTime fantasyTime = new FantasyTime(fixedClock("2026-05-08T17:00:00Z"));

        LocalDateTime dateTime = fantasyTime.getDateTime();

        assertThat(dateTime).isEqualTo(LocalDateTime.of(2026, 5, 9, 2, 0));
    }

    @Test
    void getInstant는_주입된_clock의_instant를_반환한다() {
        FantasyTime fantasyTime = new FantasyTime(fixedClock("2026-05-08T17:00:00Z"));

        Instant instant = fantasyTime.getInstant();

        assertThat(instant).isEqualTo(Instant.parse("2026-05-08T17:00:00Z"));
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), SEOUL_ZONE_ID);
    }

}
