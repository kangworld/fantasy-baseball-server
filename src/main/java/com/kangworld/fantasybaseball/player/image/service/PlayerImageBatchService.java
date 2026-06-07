package com.kangworld.fantasybaseball.player.image.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.asset.storage.AssetFile;
import com.kangworld.fantasybaseball.asset.storage.AssetStorage;
import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboPlayerImageFetcher;
import com.kangworld.fantasybaseball.player.image.dao.PlayerImageDao;
import com.kangworld.fantasybaseball.player.image.model.PlayerImage;
import com.kangworld.fantasybaseball.player.image.model.PlayerImageRefreshTarget;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerImageBatchService {

    private static final String IMG_PATH_FORMAT = "player/%d.%s";

    private final PlayerImageDao playerImageDao;
    private final KboPlayerImageFetcher kboPlayerImageFetcher;
    private final AssetStorage assetStorage;
    private final FantasyTime fantasyTime;

    @Transactional
    public void refresh(LocalDate targetDate) {
        // 이미지 배치는 점수 profile이 아니라 player 기준으로 신규/팀 변경 선수를 찾는다.
        // player.team_updated_at은 팀 코드가 실제 변경된 경우에만 갱신되므로 이미지 재확인 기준으로 사용한다.
        List<PlayerImageRefreshTarget> refreshTargets = playerImageDao.findRefreshTargets(targetDate.atStartOfDay());
        if (refreshTargets.isEmpty())
            return;

        refreshTargets.forEach(target -> refreshPlayerImageIfNeeded(
                target.getPlayerId(),
                targetDate.getYear(),
                target.toSavedImage()));
    }

    private void refreshPlayerImageIfNeeded(long playerId, int season, PlayerImage savedImage) {
        kboPlayerImageFetcher.fetch(playerId, season)
                .ifPresent(imageFile -> uploadIfChanged(playerId, savedImage, imageFile));
    }

    private void uploadIfChanged(long playerId, PlayerImage savedImage, AssetFile imageFile) {
        String imgPath = buildImgPath(playerId, imageFile.getExtension());
        // 팀 변경 감지 시에도 실제 이미지 bytes가 같으면 S3 재업로드와 DB timestamp 변경을 건너뛴다.
        if (savedImage != null
                && imgPath.equals(savedImage.getImgPath())
                && hasSameBytes(savedImage.getImgPath(), imageFile.getBytes()))
            return;

        // S3 업로드가 성공한 뒤 DB를 갱신한다. 현재 트랜잭션은 DB 변경 경계를 유지하고, S3 보상 처리는 별도 운영 과제로 둔다.
        assetStorage.upload(imgPath, imageFile);
        LocalDateTime now = fantasyTime.getDateTime();
        playerImageDao.upsert(PlayerImage.builder()
                .playerId(playerId)
                .imgPath(imgPath)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private boolean hasSameBytes(String imgPath, byte[] bytes) {
        return assetStorage.findBytes(imgPath)
                .map(savedBytes -> Arrays.equals(savedBytes, bytes))
                .orElse(false);
    }

    private String buildImgPath(long playerId, String extension) {
        return IMG_PATH_FORMAT.formatted(playerId, extension);
    }

}
