package com.kangworld.fantasybaseball.rank.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kangworld.fantasybaseball.auth.security.AuthenticatedMember;
import com.kangworld.fantasybaseball.common.api.ApiResponse;
import com.kangworld.fantasybaseball.rank.service.RankService;
import com.kangworld.fantasybaseball.rank.service.dto.DailyRankPageResponse;
import com.kangworld.fantasybaseball.rank.service.dto.ScoreboardResponse;
import com.kangworld.fantasybaseball.rank.service.dto.SeasonRankPageResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/scoreboard/me")
    public ApiResponse<ScoreboardResponse> findMyScoreboard(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(rankService.findMyScoreboard(authenticatedMember.getMemberId(), date));
    }

    @GetMapping("/ranks/daily")
    public ApiResponse<DailyRankPageResponse> findDailyRanks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(rankService.findDailyRanks(date, page, size));
    }

    @GetMapping("/ranks/season")
    public ApiResponse<SeasonRankPageResponse> findSeasonRanks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(rankService.findSeasonRanks(date, page, size));
    }

}
