package com.kangworld.fantasybaseball.rank.service;

import java.time.LocalDate;

import com.kangworld.fantasybaseball.rank.service.dto.DailyRankPageResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardResponse;
import com.kangworld.fantasybaseball.rank.service.dto.SeasonRankPageResponse;

public interface RankService {

    ScoreboardResponse findMyScoreboard(long memberId, LocalDate date);

    DailyRankPageResponse findDailyRanks(LocalDate date, int page, int size);

    SeasonRankPageResponse findSeasonRanks(LocalDate date, int page, int size);

}
