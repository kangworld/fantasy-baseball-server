package com.kangworld.fantasybaseball.player.image.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kangworld.fantasybaseball.asset.storage.AssetFile;
import com.kangworld.fantasybaseball.asset.storage.AssetStorage;
import com.kangworld.fantasybaseball.common.date.DateIds;
import com.kangworld.fantasybaseball.common.time.FantasyTime;
import com.kangworld.fantasybaseball.external.kbo.fetcher.KboPlayerImageFetcher;
import com.kangworld.fantasybaseball.player.image.dao.PlayerImageDao;
import com.kangworld.fantasybaseball.player.image.model.PlayerImage;
import com.kangworld.fantasybaseball.player.model.PlayerProfile;
import com.kangworld.fantasybaseball.point.dao.PlayerProfilePointDao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerImageBatchService {

    private static final String IMG_PATH_FORMAT = "player/%d.%s";

    private final PlayerImageDao playerImageDao;
    private final KboPlayerImageFetcher kboPlayerImageFetcher;
    private final AssetStorage assetStorage;
    private final FantasyTime fantasyTime;
    private final PlayerProfilePointDao playerProfilePointDao;

    public void refresh(LocalDate targetDate) {
        RefreshTarget refreshTarget = findRefreshTarget(targetDate);
        if (refreshTarget.playerIds().isEmpty())
            return;

        refreshTarget.playerIds()
                .forEach(playerId -> refreshPlayerImageIfNeeded(
                        playerId,
                        targetDate.getYear(),
                        refreshTarget.savedImageByPlayerId().get(playerId)));
    }

    private RefreshTarget findRefreshTarget(LocalDate targetDate) {
        // 전날/당일 profile을 한 번에 읽고 날짜별로 나눠 신규 선수와 팀 변경 선수만 이미지 확인 대상으로 좁힌다.
        int dateId = DateIds.from(targetDate);
        int previousDateId = DateIds.from(targetDate.minusDays(1));
        Map<Integer, List<PlayerProfile>> profileByDateId = playerProfilePointDao
                .findByDateIds(Set.of(previousDateId, dateId))
                .stream()
                .collect(Collectors.groupingBy(PlayerProfile::getDateId));

        List<PlayerProfile> currentProfiles = profileByDateId.getOrDefault(dateId, List.of());
        Set<Long> currentPlayerIds = currentProfiles.stream()
                .map(PlayerProfile::getPlayerId)
                .collect(Collectors.toSet());
        if (currentPlayerIds.isEmpty())
            return new RefreshTarget(Set.of(), Map.of());

        Map<Long, PlayerProfile> previousProfileByPlayerId = profileByDateId.getOrDefault(previousDateId, List.of()).stream()
                .filter(profile -> currentPlayerIds.contains(profile.getPlayerId()))
                .collect(Collectors.toMap(PlayerProfile::getPlayerId, Function.identity()));
        Map<Long, PlayerImage> savedImageByPlayerId = playerImageDao.findByPlayerIds(currentPlayerIds);

        Set<Long> playerIds = currentProfiles.stream()
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
                .ifPresent(imageFile -> uploadIfChanged(playerId, savedImage, imageFile));
    }

    private void uploadIfChanged(long playerId, PlayerImage savedImage, AssetFile imageFile) {
        String imgPath = buildImgPath(playerId, imageFile.getExtension());
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

    private String buildImgPath(long playerId, String extension) {
        return IMG_PATH_FORMAT.formatted(playerId, extension);
    }

    private record RefreshTarget(Set<Long> playerIds, Map<Long, PlayerImage> savedImageByPlayerId) {
    }

}
