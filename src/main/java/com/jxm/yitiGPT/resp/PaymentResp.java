package com.jxm.yitiGPT.resp;

import lombok.Data;

@Data
public class PaymentResp {

    // 当前 (所有历史记录和) 提问消耗的 token
    private Integer finalToken;

    // 用户类型
    private Integer userType;
}
