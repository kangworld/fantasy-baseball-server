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

import com.kangworld.fantasybaseball.rank.dao.DailyRankDao;
import com.kangworld.fantasybaseball.rank.dao.view.DailyRankView;
import com.kangworld.fantasybaseball.rank.model.DailyRank;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {"delete from daily_rank", "delete from member"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DailyRankDaoJdbcTest {

    @Autowired
    private DailyRankDao dailyRankDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        assertThat(ranks).extracting(DailyRank::getPreviousRankNo).containsExactly(0, 0);
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

    @Test
    void findViewsByDateId는_회원_이름을_포함해_페이지로_조회한다() {
        // given
        insertMember(10L, "강민구");
        insertMember(20L, "김철수");
        insertMember(30L, "박영희");
        dailyRankDao.insertAll(List.of(
                rank(20260508, 1, 10L, 100, 1, 4),
                rank(20260508, 2, 20L, 90, 2, 5),
                rank(20260508, 3, 30L, 80, 3, 6)
        ));

        // when
        List<DailyRankView> ranks = dailyRankDao.findViewsByDateId(20260508, 2, 1);

        // then
        assertThat(ranks).extracting(DailyRankView::getRankNo).containsExactly(2, 3);
        assertThat(ranks).extracting(DailyRankView::getDisplayName).containsExactly("김철수", "박영희");
        assertThat(ranks).extracting(DailyRankView::getPreviousRankNo).containsExactly(5, 6);
    }

    @Test
    void findViewByMemberIdAndDateId는_회원_이름을_포함해_단건_조회한다() {
        // given
        insertMember(10L, "강민구");
        dailyRankDao.insertAll(List.of(rank(20260508, 1, 10L, 100, 1, 3)));

        // when
        Optional<DailyRankView> rank = dailyRankDao.findViewByMemberIdAndDateId(10L, 20260508);

        // then
        assertThat(rank).isPresent();
        assertThat(rank.get().getDisplayName()).isEqualTo("강민구");
        assertThat(rank.get().getPoint()).isEqualTo(100);
        assertThat(rank.get().getPreviousRankNo()).isEqualTo(3);
    }

    private void insertMember(long memberId, String displayName) {
        jdbcTemplate.update("""
                insert into member (member_id, email, password_hash, display_name, role, status, created_at, updated_at)
                values (?, ?, 'password', ?, 'USER', 'ACTIVE', current_timestamp, current_timestamp)
                """, memberId, "member" + memberId + "@test.com", displayName);
    }

    private DailyRank rank(int dateId, int rankNo, long memberId, int point, int displayRankNo) {
        return rank(dateId, rankNo, memberId, point, displayRankNo, 0);
    }

    private DailyRank rank(int dateId, int rankNo, long memberId, int point, int displayRankNo, int previousRankNo) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 8, 23, 0);

        return DailyRank.builder()
                .dateId(dateId)
                .rankNo(rankNo)
                .memberId(memberId)
                .point(point)
                .displayRankNo(displayRankNo)
                .previousRankNo(previousRankNo)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
