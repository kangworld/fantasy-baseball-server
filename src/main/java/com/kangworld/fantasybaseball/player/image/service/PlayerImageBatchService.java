package com.kangworld.fantasybaseball.player.image.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kangworld.fantasybaseball.asset.storage.AssetFile;
import com.kangworld.fantasybaseball.asset.storage.AssetStorage;
import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboPlayerImageFetcher;
import com.kangworld.fantasybaseball.player.image.dao.PlayerImageDao;
import com.kangworld.fantasybaseball.player.image.model.PlayerImage;
import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.settlement.service.PlayerPointSettlementResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerImageBatchService {

    private static final String IMG_PATH_FORMAT = "player/%d.%s";

    private final PlayerImageDao playerImageDao;
    private final KboPlayerImageFetcher kboPlayerImageFetcher;
    private final AssetStorage assetStorage;
    private final FantasyTime fantasyTime;

    public void refresh(LocalDate targetDate, PlayerPointSettlementResult settlementResult) {
        // 포인트 정산 결과에 이미 전날/당일 프로필이 있으므로 이미지 대상 산정을 위해 profile 조인을 다시 하지 않는다.
        RefreshTarget refreshTarget = findRefreshTarget(settlementResult);
        if (refreshTarget.playerIds().isEmpty())
            return;

        refreshTarget.playerIds()
                .forEach(playerId -> refreshPlayerImageIfNeeded(
                        playerId,
                        targetDate.getYear(),
                        refreshTarget.savedImageByPlayerId().get(playerId)));
    }

    private RefreshTarget findRefreshTarget(PlayerPointSettlementResult settlementResult) {
        // 정산 결과의 전날/당일 프로필을 비교해서 신규 선수와 팀 변경 선수만 이미지 확인 대상으로 좁힌다.
        Map<Long, PlayerProfile> previousProfileByPlayerId = settlementResult.previousProfiles().stream()
                .collect(Collectors.toMap(PlayerProfile::getPlayerId, Function.identity()));
        Set<Long> currentPlayerIds = settlementResult.settledProfiles().stream()
                .map(PlayerProfile::getPlayerId)
                .collect(Collectors.toSet());
        Map<Long, PlayerImage> savedImageByPlayerId = playerImageDao.findByPlayerIds(currentPlayerIds);

        Set<Long> playerIds = settlementResult.settledProfiles()
                .stream()
                .filter(needsRefresh(previousProfileByPlayerId, savedImageByPlayerId))
                .map(PlayerProfile::getPlayerId)
                .collect(Collectors.toSet());

        return new RefreshTarget(playerIds, savedImageByPlayerId);
    }

    private Predicate<PlayerProfile> needsRefresh(
            Map<Long, PlayerProfile> previousProfileByPlayerId,
            Map<Long, PlayerImage> savedImageByPlayerId) {
        Predicate<PlayerProfile> hasNoSavedImage = profile -> !savedImageByPlayerId.containsKey(profile.getPlayerId());
        Predicate<PlayerProfile> hasChangedTeam = profile -> {
            PlayerProfile previousProfile = previousProfileByPlayerId.get(profile.getPlayerId());
            return previousProfile == null || !profile.getTeamCode().equals(previousProfile.getTeamCode());
        };

        // 저장된 이미지가 없거나 팀이 바뀐 선수만 KBO 원본 이미지를 다시 확인한다.
        return hasNoSavedImage.or(hasChangedTeam);
    }

    private void refreshPlayerImageIfNeeded(long playerId, int season, PlayerImage savedImage) {
        kboPlayerImageFetcher.fetch(playerId, season)
                .ifPresent(imageFile -> uploadIfChanged(playerId, season, savedImage, imageFile));
    }

    private void uploadIfChanged(long playerId, int season, PlayerImage savedImage, AssetFile imageFile) {
        String imgPath = buildImgPath(playerId, season, imageFile.getExtension());
        // 팀 변경 감지 시에도 실제 이미지 bytes가 같으면 S3 재업로드와 DB timestamp 변경을 건너뛴다.
        if (savedImage != null
                && imgPath.equals(savedImage.getImgPath())
                && hasSameBytes(savedImage.getImgPath(), imageFile.getBytes()))
            return;

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

    private String buildImgPath(long playerId, int season, String extension) {
        return IMG_PATH_FORMAT.formatted(playerId, extension);
    }

    private record RefreshTarget(Set<Long> playerIds, Map<Long, PlayerImage> savedImageByPlayerId) {
    }

}
