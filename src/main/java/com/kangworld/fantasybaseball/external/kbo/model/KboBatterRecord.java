package com.kangworld.fantasybaseball.external.kbo.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KboBatterRecord {

    private String playerCode;
    private String name;
    private String pos;
    private int ab;
    private int hit;
    private int rbi;
    private int run;
    private int bb;
    private int kk;
    private int hr;
    private int sb;

}
