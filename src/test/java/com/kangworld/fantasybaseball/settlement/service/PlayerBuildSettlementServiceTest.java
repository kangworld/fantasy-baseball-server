package com.kangworld.fantasybaseball.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboPlayerProfileFetcher;
import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboPlayerProfile;
import com.kangworld.fantasybaseball.external.kbo.model.KboScheduleGame;
import com.kangworld.fantasybaseball.external.kbo.service.KboGameRecordFetchService;
import com.kangworld.fantasybaseball.external.kbo.service.KboScheduleFetchService;
import com.kangworld.fantasybaseball.player.dao.PlayerDao;
import com.kangworld.fantasybaseball.player.model.Player;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerStatus;
import com.kangworld.fantasybaseball.player.model.PlayerType;

@ExtendWith(MockitoExtension.class)
class PlayerBuildSettlementServiceTest {

    @Mock
    private KboScheduleFetchService scheduleFetchService;

    @Mock
    private KboGameRecordFetchService gameRecordFetchService;

    @Mock
    private PlayerDao playerDao;

    @Mock
    private KboPlayerProfileFetcher playerProfileFetcher;

    @Mock
    private FantasyTime fantasyTime;

    @InjectMocks
    private PlayerBuildSettlementService playerBuildSettlementService;

    @Test
    void settle은_정산_대상_경기에서_등장한_선수를_player로_저장한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 6, 2);
        LocalDateTime now = LocalDateTime.of(2026, 6, 3, 2, 0);
        given(scheduleFetchService.fetch(targetDate)).willReturn(List.of(
                game("game-1", KboGameStatus.PLAYING),
                game("game-2", KboGameStatus.FINISHED)
        ));
        given(gameRecordFetchService.fetch("game-2")).willReturn(KboGameRecord.builder()
                .gameId("game-2")
                .batterRecords(List.of(batter("76290", "김현수", PlayerPosition.DH)))
                .pitcherRecords(List.of(pitcher("63492", "김광현")))
                .build());
        given(fantasyTime.getDateTime()).willReturn(now);

        // when
        playerBuildSettlementService.settle(targetDate);

        // then
        ArgumentCaptor<List<Player>> playersCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerDao).upsertAll(playersCaptor.capture());
        assertThat(playersCaptor.getValue()).extracting(Player::getId)
                .containsExactlyInAnyOrder(76290L, 63492L);
        assertThat(playersCaptor.getValue()).extracting(Player::getStatus)
                .containsOnly(PlayerStatus.ACTIVE);
        assertThat(playersCaptor.getValue()).filteredOn(player -> player.getId() == 63492L)
                .extracting(Player::getType, Player::getPosition)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(PlayerType.PITCHER, PlayerPosition.P));
        verify(gameRecordFetchService).fetch("game-2");
        verifyNoMoreInteractions(gameRecordFetchService);
    }

    @Test
    void settle은_투타_기록이_모두_있는_선수는_프로필_API의_주_포지션으로_저장한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 6, 2);
        LocalDateTime now = LocalDateTime.of(2026, 6, 3, 2, 0);
        given(scheduleFetchService.fetch(targetDate)).willReturn(List.of(game("game-2", KboGameStatus.FINISHED)));
        given(gameRecordFetchService.fetch("game-2")).willReturn(KboGameRecord.builder()
                .gameId("game-2")
                .batterRecords(List.of(batter("78168", "강승호", PlayerPosition.DH)))
                .pitcherRecords(List.of(pitcher("78168", "강승호")))
                .build());
        given(playerProfileFetcher.fetch(2026, 78168L)).willReturn(Optional.of(KboPlayerProfile.builder()
                .playerId(78168L)
                .name("강승호")
                .type(PlayerType.BATTER)
                .position(PlayerPosition.B2)
                .build()));
        given(fantasyTime.getDateTime()).willReturn(now);

        // when
        playerBuildSettlementService.settle(targetDate);

        // then
        ArgumentCaptor<List<Player>> playersCaptor = ArgumentCaptor.forClass(List.class);
        verify(playerDao).upsertAll(playersCaptor.capture());
        assertThat(playersCaptor.getValue()).singleElement()
                .extracting(Player::getType, Player::getPosition)
                .containsExactly(PlayerType.BATTER, PlayerPosition.B2);
    }

    private KboScheduleGame game(String gameId, KboGameStatus status) {
        return KboScheduleGame.builder()
                .gameId(gameId)
                .status(status)
                .build();
    }

    private KboBatterRecord batter(String playerCode, String name, PlayerPosition position) {
        return KboBatterRecord.builder()
                .playerCode(playerCode)
                .name(name)
                .position(position)
                .teamCode("LG")
                .build();
    }

    private KboPitcherRecord pitcher(String playerCode, String name) {
        return KboPitcherRecord.builder()
                .playerCode(playerCode)
                .name(name)
                .teamCode("SSG")
                .position(PlayerPosition.P)
                .build();
    }

}
