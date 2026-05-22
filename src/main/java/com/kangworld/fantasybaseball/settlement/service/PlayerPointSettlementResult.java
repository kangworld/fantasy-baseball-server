package com.kangworld.fantasybaseball.settlement.service;

import java.util.List;

import com.kangworld.fantasybaseball.player.model.PlayerProfile;

public record PlayerPointSettlementResult(
        List<PlayerProfile> previousProfiles,
        List<PlayerProfile> settledProfiles) {
}
