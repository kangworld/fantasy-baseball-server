package com.kangworld.fantasybaseball.roster.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Roster {

    private long rosterId;
    private long version;
    private long memberId;
    private int dateId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
