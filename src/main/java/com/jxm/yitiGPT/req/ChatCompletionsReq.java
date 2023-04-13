package com.jxm.yitiGPT.req;

import lombok.Data;

@Data
public class ChatCompletionsReq {
    private String prompt;
    private Integer maxTokens;
    private Double temperature;
    private Double topP;
    private String user;

}