package com.kangworld.fantasybaseball.external.kbo.model;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class KboGameStatusMapper {

    private static final Set<String> READY_CODES = Set.of("READY");
    private static final Set<String> PLAYING_CODES = Set.of("STARTED");
    private static final Set<String> FINISHED_CODES = Set.of("RESULT");

    public KboGameStatus map(String statusCode, boolean canceled, boolean suspended) {
        // cancel, suspended 플래그가 있으면 외부 statusCode보다 우선 적용.
        if (canceled) {
            return KboGameStatus.CANCELED;
        }

        if (suspended) {
            return KboGameStatus.SUSPENDED;
        }

        String normalizedCode = StringUtils.defaultString(statusCode).trim().toUpperCase();

        if (READY_CODES.contains(normalizedCode)) {
            return KboGameStatus.READY;
        }

        if (PLAYING_CODES.contains(normalizedCode)) {
            return KboGameStatus.PLAYING;
        }

        if (FINISHED_CODES.contains(normalizedCode)) {
            return KboGameStatus.FINISHED;
        }

        return KboGameStatus.UNKNOWN;
    }

}
