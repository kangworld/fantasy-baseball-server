package com.kangworld.fantasybaseball.player.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.player.dao.PlayerDao;
import com.kangworld.fantasybaseball.player.model.PlayerPosition;
import com.kangworld.fantasybaseball.player.model.PlayerSearchCondition;
import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.player.service.PlayerService;
import com.kangworld.fantasybaseball.player.service.dto.PlayerSummaryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerDao playerDao;
    private final FantasyTime fantasyTime;

    @Override
    public List<PlayerSummaryResponse> findPlayers(PlayerType playerType, PlayerPosition positionCode, String keyword) {
        PlayerSearchCondition condition = PlayerSearchCondition.builder()
                // 선수풀은 당일 로스터 구성 화면 기준이라 오늘 profile을 읽는다.
                .dateId(DateIds.from(fantasyTime.now()))
                .playerType(playerType)
                .positionCode(positionCode)
                .keyword(keyword)
                .build();

        return playerDao.findSummaries(condition).stream()
                .map(PlayerSummaryResponse::from)
                .toList();
    }

}
