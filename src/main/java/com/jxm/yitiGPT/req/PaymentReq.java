package com.jxm.yitiGPT.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentReq {
    private Long userID;

    private Long historyID;

    private String queryStr;
}
