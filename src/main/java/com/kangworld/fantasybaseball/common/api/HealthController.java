package com.kangworld.fantasybaseball.common.api;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name}") String applicationName) {
        // applicationName은 헬스 응답에서 현재 프로세스를 식별하기 위해 주입.
        this.applicationName = applicationName;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        // health 응답은 가장 단순한 스모크 체크 정보만 반환.
        return ApiResponse.ok(Map.of(
                "service", applicationName,
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()));
    }

}
