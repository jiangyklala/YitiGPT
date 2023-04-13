package com.jxm.yitiGPT.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jxm.yitiGPT.domain.MessageText;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChatChoice implements Serializable {
    private long index;

    @JsonProperty("delta")
    private MessageText delta;

    @JsonProperty("message")
    private Message message;

    @JsonProperty("finish_reason")
    private String finishReason;
}
