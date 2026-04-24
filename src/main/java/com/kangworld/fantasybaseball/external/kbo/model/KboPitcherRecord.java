package com.kangworld.fantasybaseball.external.kbo.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KboPitcherRecord {

    private String playerCode;
    private String name;
    private String inn;
    private int hit;
    private int r;
    private int er;
    private int bb;
    private int bbhp;
    private int kk;
    private int hr;
    private int bf;
    private String wls;

}
