package com.kangworld.fantasybaseball.point.service.dto;

import com.kangworld.fantasybaseball.point.model.PointRule;

public record PointRuleItemResponse(String recordCode, int point, String description) {

    public static PointRuleItemResponse from(PointRule rule) {
        return new PointRuleItemResponse(
                rule.getRecordCode().name(),
                rule.getPoint(),
                rule.getDescription()
        );
    }

}
