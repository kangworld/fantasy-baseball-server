package com.kangworld.fantasybaseball.point.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kangworld.fantasybaseball.common.api.ApiResponse;
import com.kangworld.fantasybaseball.point.service.PointRuleService;
import com.kangworld.fantasybaseball.point.service.dto.PointRuleResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/point-rules")
@RequiredArgsConstructor
public class PointRuleController {

    private final PointRuleService pointRuleService;

    @GetMapping
    public ApiResponse<PointRuleResponse> findAll() {
        return ApiResponse.ok(pointRuleService.findAll());
    }

}
