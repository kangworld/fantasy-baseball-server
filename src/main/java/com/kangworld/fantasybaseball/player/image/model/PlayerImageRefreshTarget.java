package com.kangworld.fantasybaseball.player.image.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerImageRefreshTarget {

    private long playerId;
    private String imgPath;

    public PlayerImage toSavedImage() {
        // imgPath가 없으면 아직 저장된 이미지가 없는 선수이므로 신규 업로드 대상으로 취급한다.
        if (imgPath == null || imgPath.isBlank())
            return null;

        return PlayerImage.builder()
                .playerId(playerId)
                .imgPath(imgPath)
                .build();
    }

}
