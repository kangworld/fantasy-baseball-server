package com.kangworld.fantasybaseball.player.image.dao.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.kangworld.fantasybaseball.player.image.dao.PlayerImageDao;
import com.kangworld.fantasybaseball.player.image.model.PlayerImage;

@SpringBootTest
@ActiveProfiles("test")
@Sql(statements = {
        "delete from player_image",
        "delete from player_profile",
        "delete from player"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PlayerImageDaoJdbcTest {

    @Autowired
    private PlayerImageDao playerImageDao;

    @Test
    void upsert는_선수_이미지_img_path를_저장하거나_갱신한다() {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 5, 11, 3, 10);
        PlayerImage first = image(100L, "players/2026/100.jpg", now);
        PlayerImage second = image(100L, "players/2026/100-renewed.jpg", now.plusMinutes(1));

        // when
        playerImageDao.upsert(first);
        playerImageDao.upsert(second);
        Map<Long, PlayerImage> savedImageByPlayerId = playerImageDao.findByPlayerIds(Set.of(100L));

        // then
        assertThat(savedImageByPlayerId.get(100L).getImgPath()).isEqualTo("players/2026/100-renewed.jpg");
    }

    private PlayerImage image(long playerId, String imgPath, LocalDateTime now) {
        return PlayerImage.builder()
                .playerId(playerId)
                .imgPath(imgPath)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
