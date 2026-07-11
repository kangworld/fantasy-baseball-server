package com.kangworld.fantasybaseball.point.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.player.model.PlayerType;
import com.kangworld.fantasybaseball.point.dao.PointRuleDao;
import com.kangworld.fantasybaseball.point.model.PointRule;
import com.kangworld.fantasybaseball.point.service.PointRuleService;
import com.kangworld.fantasybaseball.point.service.dto.PointRuleItemResponse;
import com.kangworld.fantasybaseball.point.service.dto.PointRuleResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointRuleServiceImpl implements PointRuleService {

    private final PointRuleDao pointRuleDao;

    @Override
    @Transactional(readOnly = true)
    public PointRuleResponse findAll() {
        // 규칙 수가 작고 운영 변경을 즉시 노출해야 하므로 애플리케이션 캐시 없이 active 규칙을 조회한다.
        List<PointRule> activeRules = pointRuleDao.findAllActive();

        return new PointRuleResponse(
                rulesFor(activeRules, PlayerType.BATTER),
                rulesFor(activeRules, PlayerType.PITCHER)
        );
    }

    private List<PointRuleItemResponse> rulesFor(List<PointRule> rules, PlayerType playerType) {
        return rules.stream()
                .filter(rule -> rule.getPlayerType() == playerType)
                .map(PointRuleItemResponse::from)
                .toList();
    }

}
