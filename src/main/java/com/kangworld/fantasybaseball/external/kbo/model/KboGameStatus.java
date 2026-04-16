package com.kangworld.fantasybaseball.external.kbo.model;

public enum KboGameStatus {

    READY,
    PLAYING,
    FINISHED,
    CANCELED,
    SUSPENDED,
    UNKNOWN;

    public boolean isLive() {
        return this == PLAYING;
    }

    public boolean supportsRecordSync() {
        return this == PLAYING || this == FINISHED || this == SUSPENDED;
    }

}
