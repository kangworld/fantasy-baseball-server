package com.kangworld.fantasybaseball.roster.service.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RosterConfigResponse {

    private int costLimit;
    private List<RosterPositionConfigResponse> positions;

}
