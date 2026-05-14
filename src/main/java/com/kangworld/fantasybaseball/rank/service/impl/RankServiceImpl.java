package com.kangworld.fantasybaseball.rank.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.rank.dao.DailyRankDao;
import com.kangworld.fantasybaseball.rank.dao.SeasonRankDao;
import com.kangworld.fantasybaseball.rank.dao.view.DailyRankView;
import com.kangworld.fantasybaseball.rank.dao.view.SeasonRankView;
import com.kangworld.fantasybaseball.rank.service.RankService;
import com.kangworld.fantasybaseball.rank.service.dto.DailyRankItemResponse;
import com.kangworld.fantasybaseball.rank.service.dto.DailyRankPageResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardDailyResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardSeasonResponse;
import com.kangworld.fantasybaseball.rank.service.dto.SeasonRankItemResponse;
import com.kangworld.fantasybaseball.rank.service.dto.SeasonRankPageResponse;
import com.kangworld.fantasybaseball.settlement.service.SettlementExecutionRecorder;

@Service
public class RankServiceImpl implements RankService {

    private static final Duration RANK_PAGE_CACHE_TTL = Duration.ofSeconds(30);
    private static final long RANK_PAGE_CACHE_MAX_SIZE = 200L;

    private final DailyRankDao dailyRankDao;
    private final SeasonRankDao seasonRankDao;
    private final SettlementExecutionRecorder settlementExecutionRecorder;
    private final FantasyTime fantasyTime;
    private final LoadingCache<RankPageKey, List<DailyRankView>> dailyRankPageCache;
    private final LoadingCache<RankPageKey, List<SeasonRankView>> seasonRankPageCache;

    public RankServiceImpl(
            DailyRankDao dailyRankDao,
            SeasonRankDao seasonRankDao,
            SettlementExecutionRecorder settlementExecutionRecorder,
            FantasyTime fantasyTime) {
        this.dailyRankDao = dailyRankDao;
        this.seasonRankDao = seasonRankDao;
        this.settlementExecutionRecorder = settlementExecutionRecorder;
        this.fantasyTime = fantasyTime;
        this.dailyRankPageCache = Caffeine.newBuilder()
                // 전체 랭킹은 여러 사용자가 같은 페이지를 반복 조회하므로 짧은 로컬 캐시만 둔다.
                .expireAfterWrite(RANK_PAGE_CACHE_TTL)
                .maximumSize(RANK_PAGE_CACHE_MAX_SIZE)
                .build(this::findDailyRankViews);
        this.seasonRankPageCache = Caffeine.newBuilder()
                .expireAfterWrite(RANK_PAGE_CACHE_TTL)
                .maximumSize(RANK_PAGE_CACHE_MAX_SIZE)
                .build(this::findSeasonRankViews);
    }

    @Override
    public ScoreboardResponse findMyScoreboard(long memberId, LocalDate date) {
        LocalDate targetDate = resolveDate(date);
        int dateId = DateIds.from(targetDate);

        if (!settlementExecutionRecorder.isDone(targetDate))
            return emptyScoreboard(targetDate);

        // 개인화 점수판은 회원 수만큼 캐시가 커지므로 초기 구현에서는 매번 필요한 row만 조회한다.
        ScoreboardDailyResponse daily = dailyRankDao.findViewByMemberIdAndDateId(memberId, dateId)
                .map(ScoreboardDailyResponse::from)
                .orElseGet(ScoreboardDailyResponse::empty);
        ScoreboardSeasonResponse season = seasonRankDao.findViewByMemberIdAndDateId(memberId, dateId)
                .map(ScoreboardSeasonResponse::from)
                .orElseGet(ScoreboardSeasonResponse::empty);

        return ScoreboardResponse.builder()
                .date(targetDate)
                .daily(daily)
                .season(season)
                .build();
    }

    @Override
    public DailyRankPageResponse findDailyRanks(LocalDate date, int page, int size) {
        LocalDate targetDate = resolveDate(date);
        if (!settlementExecutionRecorder.isDone(targetDate))
            return emptyDailyRanks(targetDate, page, size);

        RankPageKey key = RankPageKey.of(DateIds.from(targetDate), page, size);
        List<DailyRankItemResponse> ranks = dailyRankPageCache.get(key).stream()
                .map(DailyRankItemResponse::from)
                .toList();

        return DailyRankPageResponse.builder()
                .date(targetDate)
                .page(page)
                .size(size)
                .ranks(ranks)
                .build();
    }

    @Override
    public SeasonRankPageResponse findSeasonRanks(LocalDate date, int page, int size) {
        LocalDate targetDate = resolveDate(date);
        if (!settlementExecutionRecorder.isDone(targetDate))
            return emptySeasonRanks(targetDate, page, size);

        RankPageKey key = RankPageKey.of(DateIds.from(targetDate), page, size);
        List<SeasonRankItemResponse> ranks = seasonRankPageCache.get(key).stream()
                .map(SeasonRankItemResponse::from)
                .toList();

        return SeasonRankPageResponse.builder()
                .date(targetDate)
                .page(page)
                .size(size)
                .ranks(ranks)
                .build();
    }

    private LocalDate resolveDate(LocalDate date) {
        if (date != null)
            return date;

        return fantasyTime.now();
    }

    private ScoreboardResponse emptyScoreboard(LocalDate targetDate) {
        return ScoreboardResponse.builder()
                .date(targetDate)
                .daily(ScoreboardDailyResponse.empty())
                .season(ScoreboardSeasonResponse.empty())
                .build();
    }

    private DailyRankPageResponse emptyDailyRanks(LocalDate targetDate, int page, int size) {
        return DailyRankPageResponse.builder()
                .date(targetDate)
                .page(page)
                .size(size)
                .ranks(List.of())
                .build();
    }

    private SeasonRankPageResponse emptySeasonRanks(LocalDate targetDate, int page, int size) {
        return SeasonRankPageResponse.builder()
                .date(targetDate)
                .page(page)
                .size(size)
                .ranks(List.of())
                .build();
    }

    private List<DailyRankView> findDailyRankViews(RankPageKey key) {
        return List.copyOf(dailyRankDao.findViewsByDateId(key.dateId(), key.limit(), key.offset()));
    }

    private List<SeasonRankView> findSeasonRankViews(RankPageKey key) {
        return List.copyOf(seasonRankDao.findViewsByDateId(key.dateId(), key.limit(), key.offset()));
    }

    private record RankPageKey(int dateId, int page, int size) {

        static RankPageKey of(int dateId, int page, int size) {
            return new RankPageKey(dateId, page, size);
        }

        int limit() {
            return size;
        }

        int offset() {
            return page * size;
        }

    }

}
