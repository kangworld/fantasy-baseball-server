package com.kangworld.fantasybaseball.game.service.dto;

import com.kangworld.fantasybaseball.external.kbo.model.KboBatterRecord;
import com.kangworld.fantasybaseball.external.kbo.model.KboPitcherRecord;
import com.kangworld.fantasybaseball.player.model.PlayerType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerRecordResponse {

    private String gameId;
    private long playerId;
    private String name;
    private PlayerType playerType;
    private String pos;
    private Integer ab;
    private Integer hit;
    private Integer rbi;
    private Integer run;
    private Integer bb;
    private Integer kk;
    private Integer hr;
    private Integer sb;
    private String inn;
    private Integer r;
    private Integer er;
    private Integer bf;

    public static PlayerRecordResponse fromBatter(String gameId, KboBatterRecord record) {
        return PlayerRecordResponse.builder()
                .gameId(gameId)
                .playerId(toPlayerId(record.getPlayerCode()))
                .name(record.getName())
                .playerType(PlayerType.BATTER)
                .pos(record.getPos())
                .ab(record.getAb())
                .hit(record.getHit())
                .rbi(record.getRbi())
                .run(record.getRun())
                .bb(record.getBb())
                .kk(record.getKk())
                .hr(record.getHr())
                .sb(record.getSb())
                .build();
    }

    public static PlayerRecordResponse fromPitcher(String gameId, KboPitcherRecord record) {
        return PlayerRecordResponse.builder()
                .gameId(gameId)
                .playerId(toPlayerId(record.getPlayerCode()))
                .name(record.getName())
                .playerType(PlayerType.PITCHER)
                .pos("P")
                .hit(record.getHit())
                .bb(record.getBb())
                .kk(record.getKk())
                .hr(record.getHr())
                .inn(record.getInn())
                .r(record.getR())
                .er(record.getEr())
                .bf(record.getBf())
                .build();
    }

    private static long toPlayerId(String playerCode) {
        return Long.parseLong(playerCode);
    }

}
