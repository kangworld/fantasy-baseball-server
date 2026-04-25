package com.kangworld.fantasybaseball.point.dao;

import java.util.List;

import com.kangworld.fantasybaseball.point.model.PointRule;

public interface PointRuleDao {

    List<PointRule> findAllActive();

}
