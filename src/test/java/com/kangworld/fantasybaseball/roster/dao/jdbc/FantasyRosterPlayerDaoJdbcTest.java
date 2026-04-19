package com.kangworld.fantasybaseball.roster.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.roster.dao.FantasyRosterDao;
import com.kangworld.fantasybaseball.roster.dao.FantasyRosterPlayerDao;
import com.kangworld.fantasybaseball.roster.model.FantasyRoster;
import com.kangworld.fantasybaseball.roster.model.FantasyRosterPlayer;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {
        "delete from fantasy_roster_player",
        "delete from fantasy_roster"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FantasyRosterPlayerDaoJdbcTest {

    @Autowired
    private FantasyRosterDao fantasyRosterDao;

    @Autowired
    private FantasyRosterPlayerDao fantasyRosterPlayerDao;

    @Test
    void insertAll은_로스터_선수를_저장하고_slotNo_순서로_조회한다() {
        // given
        long rosterId = insertRoster();
        fantasyRosterPlayerDao.insertAll(List.of(
                buildPlayer(rosterId, 2, 78548L, PlayerType.BATTER, PlayerPosition.C),
                buildPlayer(rosterId, 1, 76290L, PlayerType.BATTER, PlayerPosition.DH),
                buildPlayer(rosterId, 3, 63492L, PlayerType.PITCHER, PlayerPosition.P)
        ));

        // when
        List<FantasyRosterPlayer> players = fantasyRosterPlayerDao.findByRosterId(rosterId);

        // then
        assertThat(players).extracting(FantasyRosterPlayer::getSlotNo)
                .containsExactly(1, 2, 3);
        assertThat(players).extracting(FantasyRosterPlayer::getPlayerId)
                .containsExactly(76290L, 78548L, 63492L);
    }

    @Test
    void insertAll은_같은_로스터에서_slotNo를_중복_저장할_수_없다() {
        // given
        long rosterId = insertRoster();
        List<FantasyRosterPlayer> players = List.of(
                buildPlayer(rosterId, 1, 76290L, PlayerType.BATTER, PlayerPosition.DH),
                buildPlayer(rosterId, 1, 78548L, PlayerType.BATTER, PlayerPosition.C)
        );

        // when then
        assertThatThrownBy(() -> fantasyRosterPlayerDao.insertAll(players))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void insertAll은_같은_로스터에서_선수를_중복_저장할_수_없다() {
        // given
        long rosterId = insertRoster();
        List<FantasyRosterPlayer> players = List.of(
                buildPlayer(rosterId, 1, 76290L, PlayerType.BATTER, PlayerPosition.DH),
                buildPlayer(rosterId, 2, 76290L, PlayerType.BATTER, PlayerPosition.DH)
        );

        // when then
        assertThatThrownBy(() -> fantasyRosterPlayerDao.insertAll(players))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertRoster() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 4, 10, 0);

        return fantasyRosterDao.insert(FantasyRoster.builder()
                .memberId(1L)
                .dateId(20260504)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private FantasyRosterPlayer buildPlayer(
            long rosterId,
            int slotNo,
            long playerId,
            PlayerType playerType,
            PlayerPosition positionCode
    ) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 4, 10, 0);

        return FantasyRosterPlayer.builder()
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
