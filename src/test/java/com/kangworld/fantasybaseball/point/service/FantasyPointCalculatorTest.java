package com.kangworld.fantasybaseball.point.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.point.model.BatterGameRecord;
import com.kangworld.fantasybaseball.point.model.BatterPointRecordCode;
import com.kangworld.fantasybaseball.point.model.CalculatedPlayerPoint;
import com.kangworld.fantasybaseball.point.model.FantasyPointRule;
import com.kangworld.fantasybaseball.point.model.PitcherGameRecord;
import com.kangworld.fantasybaseball.point.model.PitcherPointRecordCode;
import com.kangworld.fantasybaseball.point.model.PointRecordCode;

class FantasyPointCalculatorTest {

    private final FantasyPointCalculator calculator = new FantasyPointCalculator();

    @Test
    void 타자_명시필드와_문자열_기록을_포인트로_계산한다() {
        // given
        BatterGameRecord record = BatterGameRecord.builder()
                .gameId("20260506WOSS02026")
                .playerId(72443)
                .hit(3)
                .hr(1)
                .bb(1)
                .run(2)
                .rbi(4)
                .kk(1)
                .sb(1)
                .paResults(List.of("좌2", "우중3", "사구", "투희번", "중희비", "유병"))
                .cs(1)
                .runnerOut(1)
                .build();

        // when
        CalculatedPlayerPoint point = calculator.calculateBatter(record, batterRules());

        // then
        assertThat(point.point()).isEqualTo(29);
    }

    @Test
    void 투수_이닝_승패홀세_QS_부정기록을_포인트로_계산한다() {
        // given
        PitcherGameRecord record = PitcherGameRecord.builder()
                .gameId("20260506WOSS02026")
                .playerId(65320)
                .inn("7")
                .hit(3)
                .er(1)
                .bb(1)
                .bbhp(2)
                .kk(5)
                .hr(1)
                .wls("승")
                .wildPitch(1)
                .balk(1)
                .build();

        // when
        CalculatedPlayerPoint point = calculator.calculatePitcher(record, pitcherRules());

        // then
        assertThat(point.point()).isEqualTo(28);
    }

    @Test
    void QS플러스는_QS와_중복_계산하지_않는다() {
        // given
        PitcherGameRecord record = PitcherGameRecord.builder()
                .gameId("game")
                .playerId(1)
                .inn("7")
                .er(0)
                .build();

        // when
        CalculatedPlayerPoint point = calculator.calculatePitcher(record, pitcherRules());

        // then
        assertThat(point.point()).isEqualTo(27);
    }

    private List<FantasyPointRule> batterRules() {
        return List.of(
                batterRule(BatterPointRecordCode.BAT_HIT, 2),
                batterRule(BatterPointRecordCode.BAT_2B, 2),
                batterRule(BatterPointRecordCode.BAT_3B, 4),
                batterRule(BatterPointRecordCode.BAT_HR, 6),
                batterRule(BatterPointRecordCode.BAT_BB, 1),
                batterRule(BatterPointRecordCode.BAT_HBP, 1),
                batterRule(BatterPointRecordCode.BAT_RUN, 2),
                batterRule(BatterPointRecordCode.BAT_RBI, 2),
                batterRule(BatterPointRecordCode.BAT_SH, 1),
                batterRule(BatterPointRecordCode.BAT_SF, 1),
                batterRule(BatterPointRecordCode.BAT_SB, 3),
                batterRule(BatterPointRecordCode.BAT_CS, -2),
                batterRule(BatterPointRecordCode.BAT_RUNNER_OUT, -2),
                batterRule(BatterPointRecordCode.BAT_SO, -1),
                batterRule(BatterPointRecordCode.BAT_GDP, -3)
        );
    }

    private List<FantasyPointRule> pitcherRules() {
        return List.of(
                pitcherRule(PitcherPointRecordCode.PIT_OUT, 1),
                pitcherRule(PitcherPointRecordCode.PIT_SO, 2),
                pitcherRule(PitcherPointRecordCode.PIT_WIN, 5),
                pitcherRule(PitcherPointRecordCode.PIT_LOSS, -3),
                pitcherRule(PitcherPointRecordCode.PIT_HOLD, 4),
                pitcherRule(PitcherPointRecordCode.PIT_SAVE, 5),
                pitcherRule(PitcherPointRecordCode.PIT_QS, 4),
                pitcherRule(PitcherPointRecordCode.PIT_QSP, 6),
                pitcherRule(PitcherPointRecordCode.PIT_HIT, -1),
                pitcherRule(PitcherPointRecordCode.PIT_HR, -4),
                pitcherRule(PitcherPointRecordCode.PIT_BB, -1),
                pitcherRule(PitcherPointRecordCode.PIT_HBP, -1),
                pitcherRule(PitcherPointRecordCode.PIT_ER, -3),
                pitcherRule(PitcherPointRecordCode.PIT_WP, -1),
                pitcherRule(PitcherPointRecordCode.PIT_BK, -1)
        );
    }

    private FantasyPointRule batterRule(PointRecordCode recordCode, int point) {
        return rule(PlayerType.BATTER, recordCode, point);
    }

    private FantasyPointRule pitcherRule(PointRecordCode recordCode, int point) {
        return rule(PlayerType.PITCHER, recordCode, point);
    }

    private FantasyPointRule rule(PlayerType playerType, PointRecordCode recordCode, int point) {
        return FantasyPointRule.builder()
                .playerType(playerType)
                .recordCode(recordCode)
                .point(point)
                .active(true)
                .build();
    }

}
