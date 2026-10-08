package com.truongtho.be.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.unit.DataSize;

@Getter
@RequiredArgsConstructor
public enum UploadFolder {
    PRODUCT("products", DataSize.ofMegabytes(5)),
    AVATAR("avatars", DataSize.ofMegabytes(2));

    private final String path;
    private final DataSize maxSize;
}