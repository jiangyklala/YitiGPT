package com.jxm.yitiGPT.req;

import jakarta.validation.constraints.NotNull;

public class ChatCplQueryReq {

    @NotNull(message = "userID 不能为空")
    private Long userID;

    private Long historyID;

    private String queryStr;

    public Long getHistoryID() {
        return historyID;
    }

    public void setHistoryID(Long historyID) {
        this.historyID = historyID;
    }

    public Long getUserID() {
        return userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public String getQueryStr() {
        return queryStr;
    }

    public void setQueryStr(String queryStr) {
        this.queryStr = queryStr;
    }

    @Override
    public String toString() {
        return "ChatCplQueryReq{" +
                "userID=" + userID +
                ", historyID=" + historyID +
                ", queryStr='" + queryStr + '\'' +
                '}';
    }
}
