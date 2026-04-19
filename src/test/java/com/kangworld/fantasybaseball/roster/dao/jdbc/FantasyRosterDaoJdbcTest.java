package com.kangworld.fantasybaseball.roster.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.roster.dao.FantasyRosterDao;
import com.kangworld.fantasybaseball.roster.model.FantasyRoster;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {
        "delete from fantasy_roster_player",
        "delete from fantasy_roster"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FantasyRosterDaoJdbcTest {

    @Autowired
    private FantasyRosterDao fantasyRosterDao;

    @Test
    void insert는_로스터를_저장하고_생성된_id를_반환한다() {
        // given
        FantasyRoster roster = buildRoster(1L, 20260504);

        // when
        long rosterId = fantasyRosterDao.insert(roster);

        // then
        Optional<FantasyRoster> found = fantasyRosterDao.findByMemberIdAndDateId(1L, 20260504);
        assertThat(rosterId).isPositive();
        assertThat(found).isPresent();
        assertThat(found.get().getRosterId()).isEqualTo(rosterId);
        assertThat(found.get().getMemberId()).isEqualTo(1L);
        assertThat(found.get().getDateId()).isEqualTo(20260504);
    }

    @Test
    void insert는_같은_회원과_날짜의_로스터를_중복_저장할_수_없다() {
        // given
        FantasyRoster roster = buildRoster(1L, 20260504);
        fantasyRosterDao.insert(roster);

        // when then
        assertThatThrownBy(() -> fantasyRosterDao.insert(roster))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private FantasyRoster buildRoster(long memberId, int dateId) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 4, 10, 0);

        return FantasyRoster.builder()
                .memberId(memberId)
                .dateId(dateId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
