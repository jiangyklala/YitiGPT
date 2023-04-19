package com.jxm.yitiGPT.listener;

import com.jxm.yitiGPT.resp.Message;

import java.util.List;

public interface CompletedCallBack {

    /**
     * 第一次提问时的回调
     */
    void completedFirst(Message questions, String answer, Long userID, Long historyID);

    /**
     * 第二次提问时的回调
     */
    void completed(Message questions, String answer, Long userID, Long historyID, List<Message> historyList);

    void recordCost(String questions, String response, List<Message> historyList);

    void fail(String sessionId);

}