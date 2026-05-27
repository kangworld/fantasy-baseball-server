package com.kangworld.fantasybaseball.roster.service;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;

@Component
public class RosterLineupRules {

    private static final int COST_LIMIT = 40;
    private static final List<RosterPositionRule> POSITION_RULES = List.of(
            new RosterPositionRule(PlayerPosition.P, 5),
            new RosterPositionRule(PlayerPosition.C, 1),
            new RosterPositionRule(PlayerPosition.B1, 1),
            new RosterPositionRule(PlayerPosition.B2, 1),
            new RosterPositionRule(PlayerPosition.B3, 1),
            new RosterPositionRule(PlayerPosition.SS, 1),
            new RosterPositionRule(PlayerPosition.LF, 1),
            new RosterPositionRule(PlayerPosition.CF, 1),
            new RosterPositionRule(PlayerPosition.RF, 1),
            new RosterPositionRule(PlayerPosition.DH, 1)
    );

    public int getCostLimit() {
        return COST_LIMIT;
    }

    public List<RosterPositionRule> getPositionRules() {
        return POSITION_RULES;
    }

    public RosterPositionRule getPositionRule(PlayerPosition rosterPosition) {
        // 컨트롤러 응답과 제출 검증이 같은 rule을 조회해야 클라이언트 하드코딩과 서버 검증이 어긋나지 않는다.
        return POSITION_RULES.stream()
                .filter(rule -> rule.rosterPosition() == rosterPosition)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown roster position: " + rosterPosition));
    }

    public int getDisplayOrder(PlayerPosition rosterPosition) {
        return IntStream.range(0, POSITION_RULES.size())
                .filter(index -> POSITION_RULES.get(index).rosterPosition() == rosterPosition)
                .findFirst()
                .orElse(POSITION_RULES.size());
    }

    public record RosterPositionRule(
            PlayerPosition rosterPosition,
            int count) {
    }

}
