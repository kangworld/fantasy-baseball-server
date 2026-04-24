package com.kangworld.fantasybaseball.point.model;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public enum BatterPointRecordCode implements PointRecordCode {

    /**
     * 안타
     */
    BAT_HIT(BatterGameRecord::getHit),

    /**
     * 2루타
     */
    BAT_2B(record -> countPaResult(record, result -> result.matches(".*(좌중|우중|좌|중|우)2.*"))),

    /**
     * 3루타
     */
    BAT_3B(record -> countPaResult(record, result -> result.matches(".*(좌중|우중|좌|중|우)3.*"))),

    /**
     * 홈런
     */
    BAT_HR(BatterGameRecord::getHr),

    /**
     * 볼넷
     */
    BAT_BB(BatterGameRecord::getBb),

    /**
     * 몸에 맞는 공
     */
    BAT_HBP(record -> countPaResult(record, result -> result.contains("사구"))),

    /**
     * 득점
     */
    BAT_RUN(BatterGameRecord::getRun),

    /**
     * 타점
     */
    BAT_RBI(BatterGameRecord::getRbi),

    /**
     * 희생 번트
     */
    BAT_SH(record -> countPaResult(record, result -> result.contains("희번"))),

    /**
     * 희생 플라이
     */
    BAT_SF(record -> countPaResult(record, result -> result.contains("희비"))),

    /**
     * 도루
     */
    BAT_SB(BatterGameRecord::getSb),

    /**
     * 도루자
     */
    BAT_CS(BatterGameRecord::getCs),

    /**
     * 주루사
     */
    BAT_RUNNER_OUT(BatterGameRecord::getRunnerOut),

    /**
     * 삼진
     */
    BAT_SO(BatterGameRecord::getKk),

    /**
     * 병살타
     */
    BAT_GDP(record -> countPaResult(record, result -> result.contains("병")));

    private final ToIntFunction<BatterGameRecord> countFunction;

    BatterPointRecordCode(ToIntFunction<BatterGameRecord> countFunction) {
        this.countFunction = countFunction;
    }

    public int count(BatterGameRecord record) {
        return countFunction.applyAsInt(record);
    }

    private static int countPaResult(BatterGameRecord record, Predicate<String> predicate) {
        List<String> paResults = record.getPaResults();
        if (paResults == null)
            return 0;

        return (int) paResults.stream()
                .filter(predicate)
                .count();
    }

}
