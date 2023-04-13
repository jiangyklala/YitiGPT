package com.jxm.yitiGPT.controller;

import com.jxm.yitiGPT.Client.OpenAiWebClient;
import com.jxm.yitiGPT.domain.ChatHistory;
import com.jxm.yitiGPT.listener.OpenAISubscriber;
import com.jxm.yitiGPT.req.ChatCplQueryReq;
import com.jxm.yitiGPT.resp.ChatCplQueryResp;
import com.jxm.yitiGPT.resp.CommonResp;
import com.jxm.yitiGPT.service.GPTService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Controller
@RequestMapping("/gpt")
@RequiredArgsConstructor  // ?
public class GPTController {

    private static final Logger LOG = LoggerFactory.getLogger(GPTController.class);

    private final OpenAiWebClient openAiWebClient;

    @Value("${my.openai.key}")
    private String API_KEY;

    @Resource
    GPTService gptService;

    @GetMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamCompletions(String prompt, String user) {
        Assert.hasLength(user, "user不能为空");
        Assert.hasLength(prompt, "prompt不能为空");
        checkContent(prompt);

        return Flux.create(emitter -> {
            OpenAISubscriber subscriber = new OpenAISubscriber(emitter);
            Flux<String> openAiResponse =
                    openAiWebClient.getChatResponse(API_KEY, user, prompt, null, null, null);
            openAiResponse.subscribe(subscriber);
            emitter.onDispose(subscriber);
        });
    }

    public void checkContent(String prompt) {
        Assert.isTrue(Boolean.FALSE.equals(openAiWebClient.checkContent(API_KEY, prompt).block()), "您输入的内容违规");
    }

    @PostMapping("/chatCompletion")
    @ResponseBody
    public CommonResp<ChatCplQueryResp> chatCompletion(@RequestBody @Valid ChatCplQueryReq chatCplQueryReq) {
        CommonResp<ChatCplQueryResp> resp = new CommonResp<>();
        ChatCplQueryResp res = gptService.chatCompletion(chatCplQueryReq);
        if (res == null) {
            resp.setSuccess(false);
            resp.setMessage("接口出错");
        }
        resp.setContent(res);
        return resp;
    }

    @PostMapping("/chatCompletion2")
    @ResponseBody
    public CommonResp<ChatCplQueryResp> chatCompletion2(@RequestBody @Valid ChatCplQueryReq chatCplQueryReq){
        CommonResp<ChatCplQueryResp> resp = new CommonResp<>();
        ChatCplQueryResp res = gptService.chatCompletion2(chatCplQueryReq);
        if (res == null) {
            resp.setSuccess(false);
            resp.setMessage("接口超时, 请重试");
        }
        resp.setContent(res);
        return resp;
    }

    @PostMapping("/image/{prompt}")
    @ResponseBody
    public CommonResp<String> image(@PathVariable String prompt){
        CommonResp<String> resp = new CommonResp<>();
        String res = gptService.image(prompt);
        if (res == null) {
            resp.setSuccess(false);
            resp.setMessage("接口超时, 请重试");
        }
        resp.setContent(res);
        return resp;
    }

    /**
     * 查询某个用户下的所有历史记录
     * @param userID 用户 ID
     * @return
     */
    @GetMapping("/selectAllByID/{userID}")
    @ResponseBody
    public CommonResp selectAll(@PathVariable Long userID) {
        CommonResp<List<ChatHistory>> resp = new CommonResp<>();

        resp.setContent(gptService.selectAllByID(userID));

        return resp;
    }

    @GetMapping("/selectContentByID/{historyId}")
    @ResponseBody
    public CommonResp<String> selectContentByID(@PathVariable Long historyId) {
        CommonResp<String> resp = new CommonResp<>();
        resp.setContent(gptService.selectContentByID(historyId));
        return resp;
    }
}
