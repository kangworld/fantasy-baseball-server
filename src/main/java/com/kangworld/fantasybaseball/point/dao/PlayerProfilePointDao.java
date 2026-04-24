package com.kangworld.fantasybaseball.point.dao;

import java.util.List;

import com.kangworld.fantasybaseball.player.model.PlayerProfile;

public interface PlayerProfilePointDao {

    List<PlayerProfile> findByDateId(int dateId);

    void upsertAll(List<PlayerProfile> profiles);

}
