package com.jxm.yitiGPT.listener;

import com.alibaba.fastjson2.JSON;
import com.jxm.yitiGPT.domain.MessageText;
import com.jxm.yitiGPT.enmus.MessageType;
import com.jxm.yitiGPT.resp.Message;
import com.jxm.yitiGPT.resp.MessageRes;
import com.jxm.yitiGPT.resp.OpenAIResp;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.FluxSink;

public class OpenAISubscriber implements Subscriber<String>, Disposable {
    private final FluxSink<String> emitter;
    private Subscription subscription;
    private final StringBuilder sb;
    private final CompletedCallBack completedCallBack;
    private final Message questions;
    private final String API_KEY;

    private static final Logger LOG = LoggerFactory.getLogger(OpenAISubscriber.class);

    public OpenAISubscriber(FluxSink<String> emitter, String API_KEY, CompletedCallBack completedCallBack, Message questions) {
        this.emitter = emitter;
        this.API_KEY = API_KEY;
        this.completedCallBack = completedCallBack;
        this.questions = questions;
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
            LOG.info("OpenAI返回数据结束了");
            subscription.request(1);
            emitter.next(JSON.toJSONString(new MessageRes(MessageType.TEXT, "", true)));
            completedCallBack.completed(questions, API_KEY, sb.toString());
            emitter.complete();
        } else {
            OpenAIResp openAIResp = JSON.parseObject(data, OpenAIResp.class);
            LOG.info("OpenAI返回数据：{}", openAIResp);
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
        LOG.info("OpenAI返回数据完成");
        emitter.complete();
    }

    @Override
    public void dispose() {
        LOG.warn("OpenAI返回数据取消");
        emitter.complete();
    }
}