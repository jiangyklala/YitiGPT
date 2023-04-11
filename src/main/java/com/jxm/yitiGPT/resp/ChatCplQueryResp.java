package com.jxm.yitiGPT.resp;

public class ChatCplQueryResp {

    private String content;

    private Long historyID;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getHistoryID() {
        return historyID;
    }

    public void setHistoryID(Long historyID) {
        this.historyID = historyID;
    }

    @Override
    public String toString() {
        return "ChatCplQueryResp{" +
                "content='" + content + '\'' +
                ", historyID=" + historyID +
                '}';
    }
}
