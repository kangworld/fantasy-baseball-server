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
import java.util.Optional;

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
import com.kangworld.fantasybaseball.player.image.model.PlayerImageRefreshTarget;

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
    private PlayerImagePersistenceService persistenceService;

    private PlayerImageBatchService playerImageBatchService;

    @BeforeEach
    void setUp() {
        FantasyTime fantasyTime = new FantasyTime(Clock.fixed(Instant.parse("2026-05-11T18:10:00Z"), SEOUL_ZONE_ID));
        playerImageBatchService = new PlayerImageBatchService(
                playerImageDao,
                kboPlayerImageFetcher,
                assetStorage,
                fantasyTime,
                persistenceService);
    }

    @Test
    void refresh는_대상_선수_이미지를_KBO에서_받아_S3에_저장하고_DB를_갱신한다() {
        // given
        byte[] bytes = "image".getBytes();
        AssetFile imageFile = imageFile(bytes);
        given(playerImageDao.findRefreshTargets(LocalDate.of(2026, 5, 11).atStartOfDay()))
                .willReturn(List.of(target(100L, null)));
        given(kboPlayerImageFetcher.fetch(100L, 2026)).willReturn(Optional.of(imageFile));

        // when
        playerImageBatchService.refresh(LocalDate.of(2026, 5, 11));

        // then
        verify(assetStorage).upload("player/100.jpg", imageFile);
        ArgumentCaptor<PlayerImage> captor = ArgumentCaptor.forClass(PlayerImage.class);
        verify(persistenceService).upsert(captor.capture());
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
        given(playerImageDao.findRefreshTargets(LocalDate.of(2026, 5, 11).atStartOfDay()))
                .willReturn(List.of(target(100L, savedImage.getImgPath())));
        given(kboPlayerImageFetcher.fetch(100L, 2026)).willReturn(Optional.of(imageFile(bytes)));
        given(assetStorage.findBytes("player/100.jpg")).willReturn(Optional.of(bytes));

        // when
        playerImageBatchService.refresh(LocalDate.of(2026, 5, 11));

        // then
        verify(assetStorage, never()).upload(any(), any());
        verify(persistenceService, never()).upsert(any());
    }

    @Test
    void refresh는_KBO에_이미지가_없으면_저장하지_않는다() {
        // given
        given(playerImageDao.findRefreshTargets(LocalDate.of(2026, 5, 11).atStartOfDay()))
                .willReturn(List.of(target(100L, null)));
        given(kboPlayerImageFetcher.fetch(100L, 2026)).willReturn(Optional.empty());

        // when
        playerImageBatchService.refresh(LocalDate.of(2026, 5, 11));

        // then
        verify(assetStorage, never()).upload(any(), any());
        verify(persistenceService, never()).upsert(any());
    }

    @Test
    void refresh는_갱신_대상이_없으면_KBO_조회도_하지_않는다() {
        // given
        given(playerImageDao.findRefreshTargets(LocalDate.of(2026, 5, 11).atStartOfDay()))
                .willReturn(List.of());

        // when
        playerImageBatchService.refresh(LocalDate.of(2026, 5, 11));

        // then
        verify(kboPlayerImageFetcher, never()).fetch(anyLong(), anyInt());
        verify(assetStorage, never()).upload(any(), any());
        verify(persistenceService, never()).upsert(any());
    }

    private AssetFile imageFile(byte[] bytes) {
        return new AssetFile(bytes, "image/jpeg", "jpg");
    }

    private PlayerImageRefreshTarget target(long playerId, String imgPath) {
        return PlayerImageRefreshTarget.builder()
                .playerId(playerId)
                .imgPath(imgPath)
                .build();
    }

}
