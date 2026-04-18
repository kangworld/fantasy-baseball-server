package com.kangworld.fantasybaseball.game.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kangworld.fantasybaseball.common.api.ApiResponse;
import com.kangworld.fantasybaseball.external.kbo.model.KboGameStatus;
import com.kangworld.fantasybaseball.game.service.GameService;
import com.kangworld.fantasybaseball.game.service.dto.GameScheduleResponse;
import com.kangworld.fantasybaseball.game.service.dto.PlayerRecordResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("/games/schedule")
    public ApiResponse<List<GameScheduleResponse>> findSchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) KboGameStatus status
    ) {
        return ApiResponse.ok(gameService.findSchedule(date, status));
    }

    @GetMapping("/player-records")
    public ApiResponse<List<PlayerRecordResponse>> findPlayerRecords() {
        return ApiResponse.ok(gameService.findTodayPlayerRecords());
    }

}
