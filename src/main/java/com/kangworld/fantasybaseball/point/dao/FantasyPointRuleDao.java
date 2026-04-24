package com.kangworld.fantasybaseball.point.dao;

import java.util.List;

import com.kangworld.fantasybaseball.point.model.FantasyPointRule;

public interface FantasyPointRuleDao {

    List<FantasyPointRule> findAllActive();

}
