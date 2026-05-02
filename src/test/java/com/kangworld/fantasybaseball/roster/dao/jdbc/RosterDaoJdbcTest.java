package com.kangworld.fantasybaseball.roster.dao.jdbc;

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

import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.model.Roster;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {
        "delete from roster_player",
        "delete from roster"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RosterDaoJdbcTest {

    @Autowired
    private RosterDao rosterDao;

    @Test
    void insert는_로스터를_저장하고_생성된_id를_반환한다() {
        // given
        Roster roster = buildRoster(1L, 20260504);

        // when
        long rosterId = rosterDao.insert(roster);

        // then
        Optional<Roster> found = rosterDao.findByMemberIdAndDateId(1L, 20260504);
        assertThat(rosterId).isPositive();
        assertThat(found).isPresent();
        assertThat(found.get().getRosterId()).isEqualTo(rosterId);
        assertThat(found.get().getMemberId()).isEqualTo(1L);
        assertThat(found.get().getDateId()).isEqualTo(20260504);
    }

    @Test
    void insert는_같은_회원과_날짜의_로스터를_중복_저장할_수_없다() {
        // given
        Roster roster = buildRoster(1L, 20260504);
        rosterDao.insert(roster);

        // when then
        assertThatThrownBy(() -> rosterDao.insert(roster))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByDateId는_해당_날짜의_로스터를_회원_id_순서로_조회한다() {
        // given
        rosterDao.insert(buildRoster(20L, 20260504));
        rosterDao.insert(buildRoster(10L, 20260504));
        rosterDao.insert(buildRoster(30L, 20260505));

        // when
        List<Roster> rosters = rosterDao.findByDateId(20260504);

        // then
        assertThat(rosters).extracting(Roster::getMemberId)
                .containsExactly(10L, 20L);
    }

    private Roster buildRoster(long memberId, int dateId) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 4, 10, 0);

        return Roster.builder()
                .memberId(memberId)
                .dateId(dateId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
