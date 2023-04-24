package com.jxm.yitiGPT.resp;

import com.jxm.yitiGPT.enmus.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private UserType userType;
    private String message;
    private Boolean ifUse;

    public Message(UserType userType, String message) {
        this.ifUse = false;
        this.userType = userType;
        this.message = message;
    }
}
