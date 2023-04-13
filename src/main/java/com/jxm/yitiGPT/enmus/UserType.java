package com.jxm.yitiGPT.enmus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserType {
    USER("Q:%s\n"), BOT("A: %s\n\n\n");
    private final String code;
}
