package com.kangworld.fantasybaseball.point.service.dto;

import java.util.List;

public record PointRuleResponse(
        List<PointRuleItemResponse> batter,
        List<PointRuleItemResponse> pitcher
) {

    public PointRuleResponse {
        batter = List.copyOf(batter);
        pitcher = List.copyOf(pitcher);
    }

}
