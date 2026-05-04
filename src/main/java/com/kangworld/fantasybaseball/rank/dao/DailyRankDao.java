package com.kangworld.fantasybaseball.rank.dao;

import java.util.List;
import java.util.Optional;

import com.kangworld.fantasybaseball.rank.model.DailyRank;
import com.kangworld.fantasybaseball.rank.dao.view.DailyRankView;

public interface DailyRankDao {

    void insertAll(List<DailyRank> ranks);

    List<DailyRank> findByDateId(int dateId);

    Optional<DailyRank> findByMemberIdAndDateId(long memberId, int dateId);

    List<DailyRankView> findViewsByDateId(int dateId, int limit, int offset);

    Optional<DailyRankView> findViewByMemberIdAndDateId(long memberId, int dateId);

    void deleteByDateId(int dateId);

}
