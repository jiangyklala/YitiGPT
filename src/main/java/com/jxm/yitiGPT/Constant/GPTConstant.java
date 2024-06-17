package com.jxm.yitiGPT.Constant;

public interface GPTConstant {

    String HOST = "https://api.openai.com";

    String CHAT_API = HOST + "/v1/chat/completions";

    String CONTENT_AUDIT = HOST + "/v1/moderations";

    String PROXY_HOST = "https://api.bianxieai.com";

    String PROXY_CHAT_API = PROXY_HOST + "/v1/chat/completions";

    String PROXY_CONTENT_AUDIT = PROXY_HOST + "/v1/moderations";
}
