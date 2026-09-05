package com.kangworld.fantasybaseball.point.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kangworld.fantasybaseball.point.model.PlayerDailyPoint;

public interface PlayerDailyPointDao {

    void deleteByDateId(int dateId);

    void upsertAll(List<PlayerDailyPoint> dailyPoints);

    Map<Long, Integer> findPointByPlayerIds(int dateId, Set<Long> playerIds);

    Map<Long, Integer> findTotalPointByPlayerIdsBetweenDateIds(int fromDateId, int toDateId, Set<Long> playerIds);

}
