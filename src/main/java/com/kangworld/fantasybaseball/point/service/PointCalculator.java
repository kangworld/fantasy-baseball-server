package com.kangworld.fantasybaseball.point.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.point.model.BatterGameRecord;
import com.kangworld.fantasybaseball.point.model.BatterPointRecordCode;
import com.kangworld.fantasybaseball.point.model.CalculatedPlayerPoint;
import com.kangworld.fantasybaseball.point.model.PointRule;
import com.kangworld.fantasybaseball.point.model.PitcherGameRecord;
import com.kangworld.fantasybaseball.point.model.PitcherPointRecordCode;

@Component
public class PointCalculator {

    public CalculatedPlayerPoint calculateBatter(
            BatterGameRecord record,
            List<PointRule> rules) {
        // 타자 기록 개수와 타자 배점만 곱해 합산
        int point = rules.stream()
                .mapToInt(rule -> ((BatterPointRecordCode) rule.getRecordCode()).count(record) * rule.getPoint())
                .sum();

        return new CalculatedPlayerPoint(record.getGameId(), record.getPlayerId(), point);
    }

    public CalculatedPlayerPoint calculatePitcher(
            PitcherGameRecord record,
            List<PointRule> rules) {
        // 투수 기록 개수와 투수 배점만 곱해 합산
        int point = rules.stream()
                .mapToInt(rule -> ((PitcherPointRecordCode) rule.getRecordCode()).count(record) * rule.getPoint())
                .sum();

        return new CalculatedPlayerPoint(record.getGameId(), record.getPlayerId(), point);
    }

}
