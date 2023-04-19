package com.jxm.yitiGPT.listener;

import com.alibaba.fastjson2.JSON;
import com.jxm.yitiGPT.enmus.MessageType;
import com.jxm.yitiGPT.resp.Message;
import com.jxm.yitiGPT.resp.MessageRes;
import com.jxm.yitiGPT.resp.OpenAIResp;
import com.jxm.yitiGPT.utils.SnowFlakeIdWorker;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.FluxSink;

import java.util.List;

public class OpenAISubscriber implements Subscriber<String>, Disposable {
    private final FluxSink<String> emitter;
    private Subscription subscription;
    private final StringBuilder sb;                     // 每次对话返回的完整答案
    private final CompletedCallBack completedCallBack;
    private final Message questions;
    private final Long userID;
    private final Long historyID;
    private final List<Message> historyList;

    private static final Logger LOG = LoggerFactory.getLogger(OpenAISubscriber.class);

    public OpenAISubscriber(FluxSink<String> emitter, CompletedCallBack completedCallBack, Message questions, Long userID, Long historyID, List<Message> historyList) {
        this.emitter = emitter;
        this.completedCallBack = completedCallBack;
        this.questions = questions;
        this.userID = userID;
        this.historyID = historyID;
        this.historyList = historyList;
        this.sb = new StringBuilder();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(String data) {
//        LOG.info("OpenAI返回数据：{}", data);
        if ("[DONE]".equals(data)) {
//            LOG.info("OpenAI返回数据结束了");
            subscription.request(1);
            if (historyID == -1) {
                Long historyIDTmp = new SnowFlakeIdWorker().nextId();
                completedCallBack.completedFirst(questions, sb.toString(), userID, historyIDTmp);
                completedCallBack.recordCost(questions.getMessage(), sb.toString(), null);
                emitter.next(JSON.toJSONString(new MessageRes(MessageType.TEXT, historyIDTmp.toString(), true)));
            } else {
                completedCallBack.completed(questions, sb.toString(), userID, historyID, historyList);
                completedCallBack.recordCost(questions.getMessage(), sb.toString(), historyList);
                emitter.next(JSON.toJSONString(new MessageRes(MessageType.TEXT, historyID.toString(), true)));
            }
            emitter.complete();
        } else {
            OpenAIResp openAIResp = JSON.parseObject(data, OpenAIResp.class);
//            LOG.info("OpenAI返回数据：{}", openAIResp);
            String content = openAIResp.getChoices().get(0).getDelta().getContent();

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