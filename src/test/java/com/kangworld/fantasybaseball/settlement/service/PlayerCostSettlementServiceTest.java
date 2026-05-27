package com.kangworld.fantasybaseball.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.point.dao.PlayerDailyPointDao;
import com.kangworld.fantasybaseball.point.dao.PlayerProfilePointDao;

@ExtendWith(MockitoExtension.class)
class PlayerCostSettlementServiceTest {

    @Mock
    private PlayerDailyPointDao playerDailyPointDao;

    @Mock
    private PlayerProfilePointDao playerProfilePointDao;

    @InjectMocks
    private PlayerCostSettlementService playerCostSettlementService;

    @Test
    void settle은_코스트_산정일이_아니면_아무것도_갱신하지_않는다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 11);

        // when
        playerCostSettlementService.settle(targetDate);

        // then
        verifyNoInteractions(playerDailyPointDao);
        verifyNoInteractions(playerProfilePointDao);
    }

    @Test
    void settle은_직전_15일_포인트로_투수와_타자를_분리해_cost를_계산한다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 16);
        List<PlayerProfile> profiles = Stream.concat(
                LongStream.rangeClosed(1, 100).mapToObj(playerId -> profile(playerId, "P", 1)),
                LongStream.rangeClosed(101, 200).mapToObj(playerId -> profile(playerId, "C", 1))
        ).toList();
        Set<Long> playerIds = profiles.stream()
                .map(PlayerProfile::getPlayerId)
                .collect(Collectors.toSet());
        Map<Long, Integer> pointByPlayerId = profiles.stream()
                .collect(Collectors.toMap(
                        PlayerProfile::getPlayerId,
                        profile -> 1000 - (int) profile.getPlayerId()
                ));
        given(playerProfilePointDao.findByDateId(20260516)).willReturn(profiles);
        given(playerDailyPointDao.findTotalPointByPlayerIdsBetweenDateIds(20260502, 20260516, playerIds))
                .willReturn(pointByPlayerId);

        // when
        playerCostSettlementService.settle(targetDate);

        // then
        org.mockito.ArgumentCaptor<List<PlayerProfile>> profilesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(playerProfilePointDao).upsertAll(profilesCaptor.capture());
        Map<Long, PlayerProfile> resultByPlayerId = profilesCaptor.getValue().stream()
                .collect(Collectors.toMap(PlayerProfile::getPlayerId, Function.identity()));

        assertThat(resultByPlayerId.get(1L).getCost()).isEqualTo(5);
        assertThat(resultByPlayerId.get(4L).getCost()).isEqualTo(4);
        assertThat(resultByPlayerId.get(12L).getCost()).isEqualTo(3);
        assertThat(resultByPlayerId.get(27L).getCost()).isEqualTo(2);
        assertThat(resultByPlayerId.get(53L).getCost()).isEqualTo(1);
        assertThat(resultByPlayerId.get(101L).getCost()).isEqualTo(5);
        assertThat(resultByPlayerId.get(104L).getCost()).isEqualTo(4);
    }

    @Test
    void settle은_직전_15일_포인트가_없는_선수를_신규_선수_cost로_둔다() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 6, 1);
        List<PlayerProfile> profiles = List.of(profile(1L, "P", 1));
        given(playerProfilePointDao.findByDateId(20260601)).willReturn(profiles);
        given(playerDailyPointDao.findTotalPointByPlayerIdsBetweenDateIds(20260518, 20260601, Set.of(1L)))
                .willReturn(Map.of());

        // when
        playerCostSettlementService.settle(targetDate);

        // then
        org.mockito.ArgumentCaptor<List<PlayerProfile>> profilesCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(playerProfilePointDao).upsertAll(profilesCaptor.capture());
        assertThat(profilesCaptor.getValue()).extracting(PlayerProfile::getCost)
                .containsExactly(3);
    }

    private PlayerProfile profile(long playerId, String pos, int cost) {
        return PlayerProfile.builder()
                .dateId(20260516)
                .playerId(playerId)
                .teamCode("LG")
                .pos(pos)
                .cost(cost)
                .point(0)
                .avgPoint(0)
                .pointGameCount(0)
                .build();
    }

}
