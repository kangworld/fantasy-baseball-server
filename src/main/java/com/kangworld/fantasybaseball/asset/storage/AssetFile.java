package com.kangworld.fantasybaseball.asset.storage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssetFile {

    private byte[] bytes;
    private String contentType;
    private String extension;

}
