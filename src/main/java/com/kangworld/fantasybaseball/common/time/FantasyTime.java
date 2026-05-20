package com.kangworld.fantasybaseball.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FantasyTime {

    private static final LocalTime DAY_CUTOFF_TIME = LocalTime.of(3, 0);

    private final Clock clock;

    public LocalDate now() {
        LocalDateTime dateTime = getDateTime();
        // 정산 완료 전에는 사용자 화면과 제출 기준이 전날에 머물러야 한다.
        if (dateTime.toLocalTime().isBefore(DAY_CUTOFF_TIME))
            return dateTime.toLocalDate().minusDays(1);

        return dateTime.toLocalDate();
    }

    public LocalDateTime getDateTime() {
        return LocalDateTime.now(clock);
    }

    public Instant getInstant() {
        return clock.instant();
    }

}
