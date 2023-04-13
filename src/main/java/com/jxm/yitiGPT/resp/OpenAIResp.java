package com.jxm.yitiGPT.resp;

import com.theokanning.openai.Usage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OpenAIResp implements Serializable {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<ChatChoice> choices;
    private Usage usage;
}