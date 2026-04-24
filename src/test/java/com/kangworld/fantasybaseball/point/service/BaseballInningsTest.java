package com.kangworld.fantasybaseball.point.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.kangworld.fantasybaseball.point.util.BaseballInnings;

class BaseballInningsTest {

    @Test
    void 이닝_문자열을_아웃카운트로_변환한다() {
        assertThat(BaseballInnings.toOuts("6")).isEqualTo(18);
        assertThat(BaseballInnings.toOuts("0 ⅓")).isEqualTo(1);
        assertThat(BaseballInnings.toOuts("1 ⅔")).isEqualTo(5);
        assertThat(BaseballInnings.toOuts("")).isZero();
    }

}
