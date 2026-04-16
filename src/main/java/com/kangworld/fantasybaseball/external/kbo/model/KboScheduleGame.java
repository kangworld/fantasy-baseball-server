package com.kangworld.fantasybaseball.external.kbo.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KboScheduleGame {

    private String gameId;
    private LocalDateTime startAt;
    private String homeTeamCode;
    private String homeTeamName;
    private String awayTeamCode;
    private String awayTeamName;
    private String statusCode;
    private String statusInfo;
    private KboGameStatus status;
    private boolean canceled;
    private boolean suspended;

}
