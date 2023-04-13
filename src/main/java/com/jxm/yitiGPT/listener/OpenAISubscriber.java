package com.jxm.yitiGPT.listener;

import com.alibaba.fastjson2.JSON;
import com.jxm.yitiGPT.resp.OpenAIResp;
import com.theokanning.openai.OpenAiResponse;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.FluxSink;

public class OpenAISubscriber implements Subscriber<String>, Disposable {
    private final FluxSink<String> emitter;
    private Subscription subscription;

    private static final Logger LOG = LoggerFactory.getLogger(OpenAISubscriber.class);

    public OpenAISubscriber(FluxSink<String> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(String data) {
        LOG.info("OpenAI返回数据：{}", data);
        if ("[DONE]".equals(data)) {
            LOG.info("OpenAI返回数据结束了");
            emitter.next("[DONE]");
            subscription.request(1);
            emitter.complete();
        } else {
            OpenAIResp openAIResp = JSON.parseObject(data, OpenAIResp.class);
            String content = openAIResp.getChoices().get(0).getDelta().getContent();
            content = content == null ? "" : content;
            emitter.next(content);
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