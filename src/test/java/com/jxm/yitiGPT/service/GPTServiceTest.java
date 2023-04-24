package com.jxm.yitiGPT.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GPTServiceTest {

    private static Encoding enc;

    @Test
    void payForAns() {
        Encoding encoding = Encodings.newDefaultEncodingRegistry()
                .getEncodingForModel("gpt-3.5-turbo")
                .orElseThrow();
        assertEquals(27, encoding.countTokens("你好, 这个世界是如此的精彩啊")); // Expected: 27 , Actual: 19
    }
}