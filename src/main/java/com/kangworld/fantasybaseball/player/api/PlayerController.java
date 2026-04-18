package com.kangworld.fantasybaseball.player.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kangworld.fantasybaseball.common.api.ApiResponse;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.player.service.PlayerService;
import com.kangworld.fantasybaseball.player.service.dto.PlayerSummaryResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping
    public ApiResponse<List<PlayerSummaryResponse>> findPlayers(
            @RequestParam(required = false) PlayerType playerType,
            @RequestParam(required = false) String positionCode,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(playerService.findPlayers(playerType, positionCode, keyword));
    }

}
