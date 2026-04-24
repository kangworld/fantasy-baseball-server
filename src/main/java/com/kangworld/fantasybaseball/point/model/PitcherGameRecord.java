package com.kangworld.fantasybaseball.point.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PitcherGameRecord {

    // KBO gameId
    private String gameId;
    // KBO pcode를 long으로 변환한 내부 player_id
    private long playerId;
    private String playerName;
    // 투구 이닝 문자열
    private String inn;
    // 피안타
    private int hit;
    // 실점
    private int r;
    // 자책점
    private int er;
    // 볼넷
    private int bb;
    // 볼넷 + 몸에 맞는 공
    private int bbhp;
    // 탈삼진
    private int kk;
    // 피홈런
    private int hr;
    // 상대 타자 수
    private int bf;
    // 승패홀세 결과
    private String wls;
    // 폭투
    private int wildPitch;
    // 보크
    private int balk;

}
