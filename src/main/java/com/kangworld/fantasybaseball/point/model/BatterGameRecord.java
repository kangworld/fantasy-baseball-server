package com.kangworld.fantasybaseball.point.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BatterGameRecord {

    // KBO gameId
    private String gameId;
    // KBO playerCode 기반 player_id
    private long playerId;
    private String playerName;
    // 당일 출전 포지션
    private String pos;
    // 타수
    private int ab;
    // 안타
    private int hit;
    // 타점
    private int rbi;
    // 득점
    private int run;
    // 볼넷
    private int bb;
    // 삼진
    private int kk;
    // 홈런
    private int hr;
    // 도루
    private int sb;
    // inn1~inn25 타석 결과
    private List<String> paResults;
    // 도루자
    private int cs;
    // 주루사
    private int runnerOut;

}
