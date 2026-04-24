package com.kangworld.fantasybaseball.point.model;

import java.util.function.ToIntFunction;

import com.kangworld.fantasybaseball.point.util.BaseballInnings;

public enum PitcherPointRecordCode implements PointRecordCode {

    /**
     * 아웃카운트
     */
    PIT_OUT(record -> BaseballInnings.toOuts(record.getInn())),

    /**
     * 탈삼진
     */
    PIT_SO(PitcherGameRecord::getKk),

    /**
     * 승리
     */
    PIT_WIN(record -> countWls(record, "승")),

    /**
     * 패전
     */
    PIT_LOSS(record -> countWls(record, "패")),

    /**
     * 홀드
     */
    PIT_HOLD(record -> countWls(record, "홀")),

    /**
     * 세이브
     */
    PIT_SAVE(record -> countWls(record, "세")),

    /**
     * 퀄리티 스타트
     */
    PIT_QS(PitcherPointRecordCode::countQualityStart),

    /**
     * 퀄리티 스타트 플러스
     */
    PIT_QSP(PitcherPointRecordCode::countQualityStartPlus),

    /**
     * 피안타
     */
    PIT_HIT(PitcherGameRecord::getHit),

    /**
     * 피홈런
     */
    PIT_HR(PitcherGameRecord::getHr),

    /**
     * 볼넷 허용
     */
    PIT_BB(PitcherGameRecord::getBb),

    /**
     * 몸에 맞는 공 허용
     */
    PIT_HBP(record -> Math.max(0, record.getBbhp() - record.getBb())),

    /**
     * 자책점
     */
    PIT_ER(PitcherGameRecord::getEr),

    /**
     * 폭투
     */
    PIT_WP(PitcherGameRecord::getWildPitch),

    /**
     * 보크
     */
    PIT_BK(PitcherGameRecord::getBalk);

    private final ToIntFunction<PitcherGameRecord> countFunction;

    PitcherPointRecordCode(ToIntFunction<PitcherGameRecord> countFunction) {
        this.countFunction = countFunction;
    }

    public int count(PitcherGameRecord record) {
        return countFunction.applyAsInt(record);
    }

    private static int countWls(PitcherGameRecord record, String wls) {
        return wls.equals(record.getWls()) ? 1 : 0;
    }

    private static int countQualityStart(PitcherGameRecord record) {
        int outs = BaseballInnings.toOuts(record.getInn());

        // QS+와 중복 방지
        return outs >= 18 && outs < 21 && record.getEr() <= 3 ? 1 : 0;
    }

    private static int countQualityStartPlus(PitcherGameRecord record) {
        int outs = BaseballInnings.toOuts(record.getInn());

        return outs >= 21 && record.getEr() <= 3 ? 1 : 0;
    }

}
