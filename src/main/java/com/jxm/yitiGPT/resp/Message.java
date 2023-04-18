package com.jxm.yitiGPT.resp;

import com.jxm.yitiGPT.enmus.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private UserType userType;
    private String message;
    private Date date;

    public Message(UserType userType, String message) {
        this.userType = userType;
        this.message = message;
        this.date = new Date();
    }
}
