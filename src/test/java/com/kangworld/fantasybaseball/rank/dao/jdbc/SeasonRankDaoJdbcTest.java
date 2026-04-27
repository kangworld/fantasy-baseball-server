package com.kangworld.fantasybaseball.rank.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.rank.dao.SeasonRankDao;
import com.kangworld.fantasybaseball.rank.model.SeasonRank;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = "delete from season_rank", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SeasonRankDaoJdbcTest {

    @Autowired
    private SeasonRankDao seasonRankDao;

    @Test
    void insertAll은_시즌_랭킹을_rankNo_순서로_저장하고_조회한다() {
        // given
        SeasonRank first = rank(20260508, 1, 10L, 1000, 0, 1, 2);
        SeasonRank second = rank(20260508, 2, 20L, 900, 50, 2, 1);

        // when
        seasonRankDao.insertAll(List.of(second, first));

        // then
        List<SeasonRank> ranks = seasonRankDao.findByDateId(20260508);
        Optional<SeasonRank> memberRank = seasonRankDao.findByMemberIdAndDateId(10L, 20260508);

        assertThat(ranks).extracting(SeasonRank::getRankNo).containsExactly(1, 2);
        assertThat(ranks).extracting(SeasonRank::getMemberId).containsExactly(10L, 20L);
        assertThat(memberRank).isPresent();
        assertThat(memberRank.get().getPoint()).isEqualTo(1000);
        assertThat(memberRank.get().getDailyPoint()).isZero();
        assertThat(memberRank.get().getPreviousRankNo()).isEqualTo(2);
    }

    @Test
    void insertAll은_같은_날짜에_같은_회원을_중복_저장할_수_없다() {
        // given
        seasonRankDao.insertAll(List.of(rank(20260508, 1, 10L, 1000, 0, 1, 1)));

        // when then
        assertThatThrownBy(() -> seasonRankDao.insertAll(List.of(rank(20260508, 2, 10L, 900, 0, 2, 1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void insertAll은_같은_날짜에_같은_rankNo를_중복_저장할_수_없다() {
        // given
        seasonRankDao.insertAll(List.of(rank(20260508, 1, 10L, 1000, 0, 1, 1)));

        // when then
        assertThatThrownBy(() -> seasonRankDao.insertAll(List.of(rank(20260508, 1, 20L, 900, 0, 2, 2))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteByDateId는_해당_날짜_랭킹만_삭제한다() {
        // given
        seasonRankDao.insertAll(List.of(
                rank(20260508, 1, 10L, 1000, 0, 1, 1),
                rank(20260509, 1, 10L, 1100, 100, 1, 1)
        ));

        // when
        seasonRankDao.deleteByDateId(20260508);

        // then
        assertThat(seasonRankDao.findByDateId(20260508)).isEmpty();
        assertThat(seasonRankDao.findByDateId(20260509)).hasSize(1);
    }

    private SeasonRank rank(
            int dateId,
            int rankNo,
            long memberId,
            int point,
            int dailyPoint,
            int displayRankNo,
            int previousRankNo) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 23, 0);

        return SeasonRank.builder()
                .dateId(dateId)
                .rankNo(rankNo)
                .memberId(memberId)
                .point(point)
                .dailyPoint(dailyPoint)
                .displayRankNo(displayRankNo)
                .previousRankNo(previousRankNo)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
