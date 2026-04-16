package com.kangworld.fantasybaseball.external.kbo.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KboGameRecord {

    private String gameId;
    private KboGameStatus status;
    private List<KboBatterRecord> batterRecords;
    private List<KboPitcherRecord> pitcherRecords;

}
