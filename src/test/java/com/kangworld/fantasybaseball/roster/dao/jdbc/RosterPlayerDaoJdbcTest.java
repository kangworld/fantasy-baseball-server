package com.kangworld.fantasybaseball.roster.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.roster.dao.RosterDao;
import com.kangworld.fantasybaseball.roster.dao.RosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.Roster;
import com.kangworld.fantasybaseball.roster.model.RosterPlayer;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {
        "delete from roster_player",
        "delete from roster"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RosterPlayerDaoJdbcTest {

    @Autowired
    private RosterDao rosterDao;

    @Autowired
    private RosterPlayerDao rosterPlayerDao;

    @Test
    void insertAll은_로스터_선수를_저장하고_slotNo_순서로_조회한다() {
        // given
        long rosterId = insertRoster();
        rosterPlayerDao.insertAll(List.of(
                buildPlayer(rosterId, 2, 78548L, PlayerType.BATTER, PlayerPosition.C),
                buildPlayer(rosterId, 1, 76290L, PlayerType.BATTER, PlayerPosition.DH),
                buildPlayer(rosterId, 3, 63492L, PlayerType.PITCHER, PlayerPosition.P)
        ));

        // when
        List<RosterPlayer> players = rosterPlayerDao.findByRosterId(rosterId);

        // then
        assertThat(players).extracting(RosterPlayer::getSlotNo)
                .containsExactly(1, 2, 3);
        assertThat(players).extracting(RosterPlayer::getPlayerId)
                .containsExactly(76290L, 78548L, 63492L);
    }

    @Test
    void findByRosterIds는_여러_로스터_선수를_한번에_조회한다() {
        // given
        long firstRosterId = insertRoster(1L);
        long secondRosterId = insertRoster(2L);
        long otherRosterId = insertRoster(3L);
        rosterPlayerDao.insertAll(List.of(
                buildPlayer(firstRosterId, 1, 100L, PlayerType.BATTER, PlayerPosition.DH),
                buildPlayer(secondRosterId, 1, 200L, PlayerType.BATTER, PlayerPosition.C),
                buildPlayer(otherRosterId, 1, 300L, PlayerType.PITCHER, PlayerPosition.P)
        ));

        // when
        List<RosterPlayer> players = rosterPlayerDao.findByRosterIds(Set.of(firstRosterId, secondRosterId));

        // then
        assertThat(players).extracting(RosterPlayer::getRosterId)
                .containsExactly(firstRosterId, secondRosterId);
        assertThat(players).extracting(RosterPlayer::getPlayerId)
                .containsExactly(100L, 200L);
    }

    @Test
    void insertAll은_같은_로스터에서_slotNo를_중복_저장할_수_없다() {
        // given
        long rosterId = insertRoster();
        List<RosterPlayer> players = List.of(
                buildPlayer(rosterId, 1, 76290L, PlayerType.BATTER, PlayerPosition.DH),
                buildPlayer(rosterId, 1, 78548L, PlayerType.BATTER, PlayerPosition.C)
        );

        // when then
        assertThatThrownBy(() -> rosterPlayerDao.insertAll(players))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void insertAll은_같은_로스터에서_선수를_중복_저장할_수_없다() {
        // given
        long rosterId = insertRoster();
        List<RosterPlayer> players = List.of(
                buildPlayer(rosterId, 1, 76290L, PlayerType.BATTER, PlayerPosition.DH),
                buildPlayer(rosterId, 2, 76290L, PlayerType.BATTER, PlayerPosition.DH)
        );

        // when then
        assertThatThrownBy(() -> rosterPlayerDao.insertAll(players))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertRoster() {
        return insertRoster(1L);
    }

    private long insertRoster(long memberId) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 4, 10, 0);

        return rosterDao.insert(Roster.builder()
                .memberId(memberId)
                .dateId(20260504)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private RosterPlayer buildPlayer(
            long rosterId,
            int slotNo,
            long playerId,
            PlayerType playerType,
            PlayerPosition positionCode
    ) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 4, 10, 0);

        return RosterPlayer.builder()
                .rosterId(rosterId)
                .slotNo(slotNo)
                .playerId(playerId)
                .playerType(playerType)
                .positionCode(positionCode)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
