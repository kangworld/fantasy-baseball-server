package com.kangworld.fantasybaseball.external.kbo.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KboGameStatusMapperTest {

    private final KboGameStatusMapper mapper = new KboGameStatusMapper();

    @Test
    void cancel이_우선이면_CANCELED로_매핑한다() {
        // given when
        KboGameStatus status = mapper.map("RESULT", true, false);

        // then
        assertThat(status).isEqualTo(KboGameStatus.CANCELED);
    }

    @Test
    void suspended가_우선이면_SUSPENDED로_매핑한다() {
        // given when
        KboGameStatus status = mapper.map("PLAYING", false, true);

        // then
        assertThat(status).isEqualTo(KboGameStatus.SUSPENDED);
    }

    @Test
    void result는_FINISHED로_매핑한다() {
        // given when
        KboGameStatus status = mapper.map("RESULT", false, false);

        // then
        assertThat(status).isEqualTo(KboGameStatus.FINISHED);
    }

    @Test
    void ready는_READY로_매핑한다() {
        // given when
        KboGameStatus status = mapper.map("READY", false, false);

        // then
        assertThat(status).isEqualTo(KboGameStatus.READY);
    }

    @Test
    void started는_PLAYING으로_매핑한다() {
        // given when
        KboGameStatus status = mapper.map("STARTED", false, false);

        // then
        assertThat(status).isEqualTo(KboGameStatus.PLAYING);
    }

    @Test
    void before는_READY로_매핑한다() {
        // given when
        KboGameStatus status = mapper.map("BEFORE", false, false);

        // then
        assertThat(status).isEqualTo(KboGameStatus.READY);
    }

    @Test
    void 확인되지_않은_상태값은_UNKNOWN으로_매핑한다() {
        // given when
        KboGameStatus status = mapper.map("UNKNOWN_CODE", false, false);

        // then
        assertThat(status).isEqualTo(KboGameStatus.UNKNOWN);
    }

}
