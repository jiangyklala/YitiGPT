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
    void completed(String answer, Long userID, Long historyID, List<Message> historyList);

    /**
     * 记录每日提问信息
     */
    void recordCost(Integer totalToken, String response, List<Message> historyList);

    void fail(String sessionId);

}