package com.jxm.yitiGPT.listener;

import com.jxm.yitiGPT.domain.MessageText;
import com.jxm.yitiGPT.resp.Message;

public interface CompletedCallBack {

    /**
     * 完成回调
     */
    void completed(Message questions, String sessionId, String response);

    void fail(String sessionId);

}