package com.jxm.yitiGPT.listener;

import com.alibaba.fastjson2.JSON;
import com.jxm.yitiGPT.enmus.MessageType;
import com.jxm.yitiGPT.resp.Message;
import com.jxm.yitiGPT.resp.MessageRes;
import com.jxm.yitiGPT.resp.OpenAIResp;
import com.jxm.yitiGPT.utils.SnowFlakeIdWorker;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.FluxSink;

import java.util.List;

@Slf4j
public class OpenAISubscriber implements Subscriber<String>, Disposable {
    private final FluxSink<String> emitter;
    private Subscription subscription;
    private final StringBuilder sb;                      // 每次对话返回的完整答案
    private final CompletedCallBack completedCallBack;   // 回调接口
    private final Message questions;                     // 用户的问题
    private final Long userID;                           // userID
    private final Long historyID;                        // 历史记录 ID
    private final List<Message> historyList;             // 历史记录 List
    private final Integer totalToken;                    // 当次对话的, 最终提问需要消耗的总 token
    private final Integer userType;                      // 用户类型

    private static final Logger LOG = LoggerFactory.getLogger(OpenAISubscriber.class);

    public OpenAISubscriber(FluxSink<String> emitter, CompletedCallBack completedCallBack, Message questions, Long userID, Integer userType, Long historyID, Integer totalToken, List<Message> historyList) {
        this.emitter = emitter;
        this.completedCallBack = completedCallBack;
        this.questions = questions;
        this.userID = userID;
        this.userType = userType;
        this.historyID = historyID;
        this.historyList = historyList;
        this.totalToken = totalToken;
        this.sb = new StringBuilder();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(String data) {
        if ("[DONE]".equals(data)) {
            subscription.request(1);
            if (historyID == -1) {
                Long historyIDTmp = new SnowFlakeIdWorker().nextId();
                completedCallBack.completedFirst(questions, sb.toString(), userID, historyIDTmp);
                completedCallBack.recordCost(userID, userType, totalToken, sb.toString(), null);

                // 最后将 historyIDTmp 发出去
                emitter.next(JSON.toJSONString(new MessageRes(MessageType.TEXT, historyIDTmp.toString(), true)));
            } else {
                completedCallBack.completed(sb.toString(), userID, historyID, historyList);
                completedCallBack.recordCost(userID, userType, totalToken, sb.toString(), historyList);
                emitter.next(JSON.toJSONString(new MessageRes(MessageType.TEXT, historyID.toString(), true)));
            }
            emitter.complete();
        } else {
            OpenAIResp openAIResp = JSON.parseObject(data, OpenAIResp.class);
            String content = openAIResp.getChoices().get(0).getDelta().getContent();
//            log.info("data: {}", content);
            content = content == null ? "" : content;

            emitter.next(JSON.toJSONString(new MessageRes(MessageType.TEXT, content, null)));
            sb.append(content);
            subscription.request(1);
        }

    }

    @Override
    public void onError(Throwable t) {
        LOG.error("OpenAI返回数据异常：{}", t.getMessage());
        emitter.error(t);
    }

    @Override
    public void onComplete() {
//        LOG.info("OpenAI返回数据完成");
        emitter.complete();
    }

    @Override
    public void dispose() {
//        LOG.warn("OpenAI返回数据取消");
        emitter.complete();
    }
}