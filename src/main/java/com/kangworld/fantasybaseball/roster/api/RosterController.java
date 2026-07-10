package com.kangworld.fantasybaseball.roster.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kangworld.fantasybaseball.abuse.AbuseLimit;
import com.kangworld.fantasybaseball.auth.security.AuthenticatedMember;
import com.kangworld.fantasybaseball.common.api.ApiResponse;
import com.kangworld.fantasybaseball.game.service.dto.PlayerRecordResponse;
import com.kangworld.fantasybaseball.roster.service.RosterService;
import com.kangworld.fantasybaseball.roster.service.dto.RosterResponse;
import com.kangworld.fantasybaseball.roster.service.dto.RosterSubmitRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rosters/me")
@RequiredArgsConstructor
public class RosterController {

    private final RosterService rosterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AbuseLimit(policy = "roster-submit", key = "#authenticatedMember.memberId()")
    public ApiResponse<RosterResponse> submit(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody RosterSubmitRequest request) {
        RosterResponse response = rosterService.submit(authenticatedMember.memberId(), request);

        return ApiResponse.of(HttpStatus.CREATED, "로스터 제출이 완료되었습니다.", response);
    }

    @PutMapping
    @AbuseLimit(policy = "roster-submit", key = "#authenticatedMember.memberId()")
    public ApiResponse<RosterResponse> update(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody RosterSubmitRequest request) {
        RosterResponse response = rosterService.update(authenticatedMember.memberId(), request);

        return ApiResponse.of(HttpStatus.OK, "로스터 수정이 완료되었습니다.", response);
    }

    @GetMapping
    public ApiResponse<RosterResponse> find(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam LocalDate date) {

        return ApiResponse.ok(rosterService.find(authenticatedMember.memberId(), date));
    }

    @GetMapping("/player-records")
    public ApiResponse<List<PlayerRecordResponse>> findPlayerRecords(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember) {

        return ApiResponse.ok(rosterService.findMyPlayerRecords(authenticatedMember.memberId()));
    }

}
