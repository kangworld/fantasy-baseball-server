package com.kangworld.fantasybaseball.rank.dao;

import java.util.List;
import java.util.Optional;

import com.kangworld.fantasybaseball.rank.model.SeasonRank;
import com.kangworld.fantasybaseball.rank.dao.view.SeasonRankView;

public interface SeasonRankDao {

    void insertAll(List<SeasonRank> ranks);

    List<SeasonRank> findByDateId(int dateId);

    Optional<SeasonRank> findByMemberIdAndDateId(long memberId, int dateId);

    List<SeasonRankView> findViewsByDateId(int dateId, int limit, int offset);

    Optional<SeasonRankView> findViewByMemberIdAndDateId(long memberId, int dateId);

    void deleteByDateId(int dateId);

}
