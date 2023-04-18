package com.jxm.yitiGPT.listener;

import com.jxm.yitiGPT.resp.Message;

import java.util.List;

public interface CompletedCallBack {

    /**
     * 完成回调
     */
    void completedFirst(Message questions, String response, Long userID, Long historyID);

    void completed(Message questions, String response, Long userID, Long historyID, List<Message> historyList);

    void fail(String sessionId);

}