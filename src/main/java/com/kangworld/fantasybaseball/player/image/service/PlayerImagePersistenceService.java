package com.kangworld.fantasybaseball.player.image.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kangworld.fantasybaseball.player.image.dao.PlayerImageDao;
import com.kangworld.fantasybaseball.player.image.model.PlayerImage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerImagePersistenceService {

    private final PlayerImageDao playerImageDao;

    @Transactional
    public void upsert(PlayerImage playerImage) {
        playerImageDao.upsert(playerImage);
    }

}
