package com.kangworld.fantasybaseball.rank.dao.view;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyRankView {

    private int rankNo;
    private int displayRankNo;
    private long memberId;
    private String displayName;
    private int point;

}
