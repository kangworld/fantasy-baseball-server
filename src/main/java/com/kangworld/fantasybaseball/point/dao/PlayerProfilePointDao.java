package com.kangworld.fantasybaseball.point.dao;

import java.util.List;
import java.util.Set;

import com.kangworld.fantasybaseball.player.model.PlayerProfile;

public interface PlayerProfilePointDao {

    List<PlayerProfile> findByDateId(int dateId);

    List<PlayerProfile> findByDateIds(Set<Integer> dateIds);

    void insertAll(List<PlayerProfile> profiles);

    void updateCostAll(List<PlayerProfile> profiles);

}
