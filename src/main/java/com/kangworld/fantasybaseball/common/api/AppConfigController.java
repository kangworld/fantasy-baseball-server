package com.kangworld.fantasybaseball.common.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kangworld.fantasybaseball.asset.storage.config.AssetStorageProperties;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/app-config")
@RequiredArgsConstructor
public class AppConfigController {

    private final AssetStorageProperties assetStorageProperties;

    @GetMapping
    public ApiResponse<AppConfigResponse> find() {
        // 웹 클라이언트는 이 기준 URL과 각 API의 imgPath를 조합해서 공개 asset을 요청한다.
        return ApiResponse.ok(AppConfigResponse.builder()
                .assetPublicBaseUrl(assetStorageProperties.getPublicBaseUrl())
                .build());
    }

}
