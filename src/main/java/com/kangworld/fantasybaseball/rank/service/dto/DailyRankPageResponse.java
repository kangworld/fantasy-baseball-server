package com.kangworld.fantasybaseball.rank.service.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyRankPageResponse {

    private LocalDate date;
    private int page;
    private int size;
    private List<DailyRankItemResponse> ranks;

}
