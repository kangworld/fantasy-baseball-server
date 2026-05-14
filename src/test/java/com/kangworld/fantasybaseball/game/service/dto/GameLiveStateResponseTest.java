package com.kangworld.fantasybaseball.game.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.external.kbo.model.KboLiveGameState;

class GameLiveStateResponseTest {

    @Test
    void READY는_currentInning이_있어도_이닝으로_사용하지_않는다() {
        // given
        KboLiveGameState state = liveState(KboGameStatus.READY, "READY", "경기전", "1회초");

        // when
        GameLiveStateResponse response = GameLiveStateResponse.from(state);

        // then
        assertThat(response.getInning()).isNull();
        assertThat(response.getTopInning()).isNull();
    }

    @Test
    void STARTED는_currentInning을_이닝과_초말로_분리한다() {
        // given
        KboLiveGameState state = liveState(KboGameStatus.PLAYING, "STARTED", "3회초", "3회초");

        // when
        GameLiveStateResponse response = GameLiveStateResponse.from(state);

        // then
        assertThat(response.getInning()).isEqualTo(3);
        assertThat(response.getTopInning()).isTrue();
    }

    @Test
    void STARTED는_currentInning이_없으면_statusInfo를_이닝으로_사용한다() {
        // given
        KboLiveGameState state = liveState(KboGameStatus.PLAYING, "STARTED", "2회말", null);

        // when
        GameLiveStateResponse response = GameLiveStateResponse.from(state);

        // then
        assertThat(response.getInning()).isEqualTo(2);
        assertThat(response.getTopInning()).isFalse();
    }

    private KboLiveGameState liveState(
            KboGameStatus status,
            String statusCode,
            String statusInfo,
            String currentInning) {
        return KboLiveGameState.builder()
                .gameId("game-1")
                .status(status)
                .statusCode(statusCode)
                .statusInfo(statusInfo)
                .currentInning(currentInning)
                .build();
    }

}
