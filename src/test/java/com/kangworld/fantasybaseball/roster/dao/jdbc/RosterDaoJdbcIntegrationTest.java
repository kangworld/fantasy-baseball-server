package com.kangworld.fantasybaseball.roster.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.model.Roster;
import com.kangworld.fantasybaseball.support.MySqlContainerIntegrationTest;

@MySqlContainerIntegrationTest
@Sql(statements = {
        "delete from roster_player",
        "delete from roster"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RosterDaoJdbcIntegrationTest {

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

    /** 두 요청에 같은 버전이 들어오면 MySQL에서 한 요청만 버전을 올릴 수 있다. */
    @Test
    void onlyOneWriteCanAdvanceTheSameMysqlVersion() {
        long id = rosterDao.insert(buildRoster(1L, 20260504));
        LocalDateTime updateTime = LocalDateTime.of(2026, 5, 4, 11, 0);

        assertThat(rosterDao.findByMemberIdAndDateId(1L, 20260504).orElseThrow().getVersion()).isZero();
        assertThat(rosterDao.advanceVersion(id, 0, updateTime)).isTrue();
        assertThat(rosterDao.advanceVersion(id, 0, updateTime.plusMinutes(1))).isFalse();
        Roster saved = rosterDao.findByMemberIdAndDateId(1L, 20260504).orElseThrow();
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getUpdatedAt()).isEqualTo(updateTime);
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
