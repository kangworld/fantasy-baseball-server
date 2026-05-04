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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.rank.dao.SeasonRankDao;
import com.kangworld.fantasybaseball.rank.dao.view.SeasonRankView;
import com.kangworld.fantasybaseball.rank.model.SeasonRank;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {"delete from season_rank", "delete from member"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SeasonRankDaoJdbcTest {

    @Autowired
    private SeasonRankDao seasonRankDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void findViewsByDateId는_회원_이름을_포함해_페이지로_조회한다() {
        // given
        insertMember(10L, "강민구");
        insertMember(20L, "김철수");
        insertMember(30L, "박영희");
        seasonRankDao.insertAll(List.of(
                rank(20260508, 1, 10L, 1000, 100, 1, 2),
                rank(20260508, 2, 20L, 900, 90, 2, 3),
                rank(20260508, 3, 30L, 800, 80, 3, 1)
        ));

        // when
        List<SeasonRankView> ranks = seasonRankDao.findViewsByDateId(20260508, 2, 1);

        // then
        assertThat(ranks).extracting(SeasonRankView::getRankNo).containsExactly(2, 3);
        assertThat(ranks).extracting(SeasonRankView::getDisplayName).containsExactly("김철수", "박영희");
        assertThat(ranks).extracting(SeasonRankView::getPreviousRankNo).containsExactly(3, 1);
    }

    @Test
    void findViewByMemberIdAndDateId는_회원_이름을_포함해_단건_조회한다() {
        // given
        insertMember(10L, "강민구");
        seasonRankDao.insertAll(List.of(rank(20260508, 1, 10L, 1000, 100, 1, 2)));

        // when
        Optional<SeasonRankView> rank = seasonRankDao.findViewByMemberIdAndDateId(10L, 20260508);

        // then
        assertThat(rank).isPresent();
        assertThat(rank.get().getDisplayName()).isEqualTo("강민구");
        assertThat(rank.get().getPoint()).isEqualTo(1000);
        assertThat(rank.get().getDailyPoint()).isEqualTo(100);
    }

    private void insertMember(long memberId, String displayName) {
        jdbcTemplate.update("""
                insert into member (member_id, email, password_hash, display_name, role, status, email_verified, created_at, updated_at)
                values (?, ?, 'password', ?, 'USER', 'ACTIVE', true, current_timestamp, current_timestamp)
                """, memberId, "member" + memberId + "@test.com", displayName);
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
