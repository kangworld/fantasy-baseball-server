package com.kangworld.fantasybaseball.roster.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kangworld.fantasybaseball.common.api.ApiResponse;
import com.kangworld.fantasybaseball.roster.service.RosterLineupRules;
import com.kangworld.fantasybaseball.roster.service.dto.RosterConfigResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterPositionConfigResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rosters/config")
@RequiredArgsConstructor
public class RosterConfigController {

    private final RosterLineupRules rosterLineupRules;

    @GetMapping
    public ApiResponse<RosterConfigResponse> find() {
        // 클라이언트 라인업 보드는 이 응답을 기준으로 칸을 그리고, 서버 제출 검증도 같은 규칙 객체를 사용한다.
        RosterConfigResponse response = RosterConfigResponse.builder()
                .costLimit(rosterLineupRules.getCostLimit())
                .positions(rosterLineupRules.getPositionRules().stream()
                        .map(rule -> RosterPositionConfigResponse.builder()
                                .rosterPosition(rule.rosterPosition())
                                .count(rule.count())
                                .build())
                        .toList())
                .build();

        return ApiResponse.ok(response);
    }

}
