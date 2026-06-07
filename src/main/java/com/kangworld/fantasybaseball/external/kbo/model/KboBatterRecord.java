package com.kangworld.fantasybaseball.external.kbo.model;

import java.util.List;

import com.kangworld.fantasybaseball.player.model.PlayerPosition;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KboBatterRecord {

    private String playerCode;
    private String name;
    private String teamCode;
    private String pos;
    private PlayerPosition position;
    private int ab;
    private int hit;
    private int rbi;
    private int run;
    private int bb;
    private int kk;
    private int hr;
    private int sb;
    private List<String> paResults;

}
