package com.kangworld.fantasybaseball.player.image.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kangworld.fantasybaseball.asset.storage.AssetFile;
import com.kangworld.fantasybaseball.asset.storage.AssetStorage;
import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboPlayerImageFetcher;
import com.kangworld.fantasybaseball.player.image.dao.PlayerImageDao;
import com.kangworld.fantasybaseball.player.image.model.PlayerImage;
import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.point.dao.PlayerProfilePointDao;

@ExtendWith(MockitoExtension.class)
class PlayerImageBatchServiceTest {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Mock
    private PlayerImageDao playerImageDao;

    @Mock
    private KboPlayerImageFetcher kboPlayerImageFetcher;

    @Mock
    private AssetStorage assetStorage;

    @Mock
    private PlayerProfilePointDao playerProfilePointDao;

    private PlayerImageBatchService playerImageBatchService;

    @BeforeEach
    void setUp() {
        FantasyTime fantasyTime = new FantasyTime(Clock.fixed(Instant.parse("2026-05-11T18:10:00Z"), SEOUL_ZONE_ID));
        playerImageBatchService = new PlayerImageBatchService(
                playerImageDao,
                kboPlayerImageFetcher,
                assetStorage,
                fantasyTime,
                playerProfilePointDao);
    }

    @Test
    void refresh는_대상_선수_이미지를_KBO에서_받아_S3에_저장하고_DB를_갱신한다() {
        // given
        byte[] bytes = "image".getBytes();
        AssetFile imageFile = imageFile(bytes);
        given(playerProfilePointDao.findByDateIds(Set.of(20260510, 20260511)))
                .willReturn(List.of(profile(20260510, 100L, "LG"), profile(20260511, 100L, "LG")));
        given(playerImageDao.findByPlayerIds(Set.of(100L))).willReturn(Map.of());
        given(kboPlayerImageFetcher.fetch(100L, 2026)).willReturn(Optional.of(imageFile));

        // when
        playerImageBatchService.refresh(LocalDate.of(2026, 5, 11));

        // then
        verify(assetStorage).upload("player/100.jpg", imageFile);
        ArgumentCaptor<PlayerImage> captor = ArgumentCaptor.forClass(PlayerImage.class);
        verify(playerImageDao).upsert(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getImgPath())
                .isEqualTo("player/100.jpg");
    }

    @Test
    void refresh는_기존_S3_이미지와_같으면_업로드와_DB갱신을_건너뛴다() {
        // given
        byte[] bytes = "same-image".getBytes();
        PlayerImage savedImage = PlayerImage.builder()
                .playerId(100L)
                .imgPath("player/100.jpg")
                .build();
        given(playerProfilePointDao.findByDateIds(Set.of(20260510, 20260511)))
                .willReturn(List.of(profile(20260510, 100L, "LG"), profile(20260511, 100L, "KT")));
        given(playerImageDao.findByPlayerIds(Set.of(100L))).willReturn(Map.of(100L, savedImage));
        given(kboPlayerImageFetcher.fetch(100L, 2026)).willReturn(Optional.of(imageFile(bytes)));
        given(assetStorage.findBytes("player/100.jpg")).willReturn(Optional.of(bytes));

        // when
        playerImageBatchService.refresh(LocalDate.of(2026, 5, 11));

        // then
        verify(assetStorage, never()).upload(any(), any());
        verify(playerImageDao, never()).upsert(any());
    }

    @Test
    void refresh는_KBO에_이미지가_없으면_저장하지_않는다() {
        // given
        given(playerProfilePointDao.findByDateIds(Set.of(20260510, 20260511)))
                .willReturn(List.of(profile(20260510, 100L, "LG"), profile(20260511, 100L, "LG")));
        given(playerImageDao.findByPlayerIds(Set.of(100L))).willReturn(Map.of());
        given(kboPlayerImageFetcher.fetch(100L, 2026)).willReturn(Optional.empty());

        // when
        playerImageBatchService.refresh(LocalDate.of(2026, 5, 11));

        // then
        verify(assetStorage, never()).upload(any(), any());
        verify(playerImageDao, never()).upsert(any());
    }

    @Test
    void refresh는_이미지가_있고_팀이_같으면_KBO_조회도_하지_않는다() {
        // given
        PlayerImage savedImage = PlayerImage.builder()
                .playerId(100L)
                .imgPath("player/100.jpg")
                .build();
        given(playerProfilePointDao.findByDateIds(Set.of(20260510, 20260511)))
                .willReturn(List.of(profile(20260510, 100L, "LG"), profile(20260511, 100L, "LG")));
        given(playerImageDao.findByPlayerIds(Set.of(100L))).willReturn(Map.of(100L, savedImage));

        // when
        playerImageBatchService.refresh(LocalDate.of(2026, 5, 11));

        // then
        verify(kboPlayerImageFetcher, never()).fetch(anyLong(), anyInt());
        verify(assetStorage, never()).upload(any(), any());
        verify(playerImageDao, never()).upsert(any());
    }

    private AssetFile imageFile(byte[] bytes) {
        return AssetFile.builder()
                .bytes(bytes)
                .contentType("image/jpeg")
                .extension("jpg")
                .build();
    }

    private PlayerProfile profile(long playerId, String teamCode) {
        return profile(20260511, playerId, teamCode);
    }

    private PlayerProfile profile(int dateId, long playerId, String teamCode) {
        return PlayerProfile.builder()
                .dateId(dateId)
                .playerId(playerId)
                .teamCode(teamCode)
                .build();
    }

}
