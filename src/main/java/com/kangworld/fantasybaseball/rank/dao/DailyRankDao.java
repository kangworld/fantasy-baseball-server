package com.kangworld.fantasybaseball.rank.dao;

import java.util.List;
import java.util.Optional;

import com.kangworld.fantasybaseball.rank.model.DailyRank;

public interface DailyRankDao {

    void insertAll(List<DailyRank> ranks);

    List<DailyRank> findByDateId(int dateId);

    Optional<DailyRank> findByMemberIdAndDateId(long memberId, int dateId);

    void deleteByDateId(int dateId);

}
