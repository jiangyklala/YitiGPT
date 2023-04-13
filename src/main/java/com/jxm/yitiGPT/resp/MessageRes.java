package com.jxm.yitiGPT.resp;

import com.jxm.yitiGPT.enmus.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRes {
    private MessageType messageType;
    private String message;
    private Boolean end;

}