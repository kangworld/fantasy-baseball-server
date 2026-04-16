package com.kangworld.fantasybaseball.external.kbo.fetcher;

import java.time.LocalDate;
import java.util.List;

import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;

public interface KboScheduleFetcher {

    List<KboScheduleGame> fetch(LocalDate fromDate, LocalDate toDate);

}
