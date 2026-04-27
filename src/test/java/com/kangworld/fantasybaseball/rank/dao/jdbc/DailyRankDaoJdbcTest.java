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

import com.kangworld.fantasybaseball.rank.dao.DailyRankDao;
import com.kangworld.fantasybaseball.rank.model.DailyRank;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = "delete from daily_rank", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DailyRankDaoJdbcTest {

    @Autowired
    private DailyRankDao dailyRankDao;

    @Test
    void insertAll은_일간_랭킹을_rankNo_순서로_저장하고_조회한다() {
        // given
        DailyRank first = rank(20260508, 1, 10L, 100, 1);
        DailyRank second = rank(20260508, 2, 20L, 80, 2);

        // when
        dailyRankDao.insertAll(List.of(second, first));

        // then
        List<DailyRank> ranks = dailyRankDao.findByDateId(20260508);
        Optional<DailyRank> memberRank = dailyRankDao.findByMemberIdAndDateId(10L, 20260508);

        assertThat(ranks).extracting(DailyRank::getRankNo).containsExactly(1, 2);
        assertThat(ranks).extracting(DailyRank::getMemberId).containsExactly(10L, 20L);
        assertThat(memberRank).isPresent();
        assertThat(memberRank.get().getPoint()).isEqualTo(100);
    }

    @Test
    void insertAll은_같은_날짜에_같은_회원을_중복_저장할_수_없다() {
        // given
        dailyRankDao.insertAll(List.of(rank(20260508, 1, 10L, 100, 1)));

        // when then
        assertThatThrownBy(() -> dailyRankDao.insertAll(List.of(rank(20260508, 2, 10L, 80, 2))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void insertAll은_같은_날짜에_같은_rankNo를_중복_저장할_수_없다() {
        // given
        dailyRankDao.insertAll(List.of(rank(20260508, 1, 10L, 100, 1)));

        // when then
        assertThatThrownBy(() -> dailyRankDao.insertAll(List.of(rank(20260508, 1, 20L, 80, 2))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteByDateId는_해당_날짜_랭킹만_삭제한다() {
        // given
        dailyRankDao.insertAll(List.of(
                rank(20260508, 1, 10L, 100, 1),
                rank(20260509, 1, 10L, 120, 1)
        ));

        // when
        dailyRankDao.deleteByDateId(20260508);

        // then
        assertThat(dailyRankDao.findByDateId(20260508)).isEmpty();
        assertThat(dailyRankDao.findByDateId(20260509)).hasSize(1);
    }

    private DailyRank rank(int dateId, int rankNo, long memberId, int point, int displayRankNo) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 23, 0);

        return DailyRank.builder()
                .dateId(dateId)
                .rankNo(rankNo)
                .memberId(memberId)
                .point(point)
                .displayRankNo(displayRankNo)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
