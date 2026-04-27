package com.kangworld.fantasybaseball.rank.dao;

import java.util.List;
import java.util.Optional;

import com.kangworld.fantasybaseball.rank.model.SeasonRank;

public interface SeasonRankDao {

    void insertAll(List<SeasonRank> ranks);

    List<SeasonRank> findByDateId(int dateId);

    Optional<SeasonRank> findByMemberIdAndDateId(long memberId, int dateId);

    void deleteByDateId(int dateId);

}
