package com.jxm.yitiGPT.resp;

import com.jxm.yitiGPT.enmus.MessageType;
import com.jxm.yitiGPT.enmus.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private MessageType messageType;
    private UserType userType;
    private String message;
    private Date date;

    public Message(MessageType messageType, UserType userType, String message) {
        this.messageType = messageType;
        this.userType = userType;
        this.message = message;
        this.date = new Date();
    }
}
