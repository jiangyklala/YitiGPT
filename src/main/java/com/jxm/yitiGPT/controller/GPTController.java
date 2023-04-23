package com.jxm.yitiGPT.controller;

import com.jxm.yitiGPT.Client.OpenAiWebClient;
import com.jxm.yitiGPT.domain.ChatHistory;
import com.jxm.yitiGPT.req.ChatCplQueryReq;
import com.jxm.yitiGPT.resp.ChatCplQueryResp;
import com.jxm.yitiGPT.resp.CommonResp;
import com.jxm.yitiGPT.service.GPTService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/gpt")
@RequiredArgsConstructor  // ?
public class GPTController {

    @Resource
    private GPTService gptService;

    @Resource
    private OpenAiWebClient openAiWebClient;

    /**
     * 内容检测
     *
     * @param queryStr
     * @return
     */
    @GetMapping("/checkContent/{queryStr}")
    public Mono<ServerResponse> checkContent(@PathVariable String queryStr) {
        byte[] decodeBase64QueryStr = Base64.getDecoder().decode(queryStr);
        String decodeQueryStr = URLDecoder.decode(new String(decodeBase64QueryStr), StandardCharsets.UTF_8);
        log.info("req:{}", decodeQueryStr);
        return openAiWebClient.checkContent(decodeQueryStr);
    }

    /**
     * 对用户进行权限验证: 永久会员通行, 普通用户和普通会员扣费
     *
     * @param userID    用户 ID
     * @param historyID 记录 ID
     */
    @GetMapping("/payForAns/{userID}/{historyID}")
    @ResponseBody
    public CommonResp payForAns(@PathVariable Long userID, @PathVariable Long historyID) {
        CommonResp resp = new CommonResp();
        gptService.payForAns(userID, historyID, resp);
        return resp;
    }

    /**
     * chatGPT 长对话接口, 以流操作返回
     *
     * @param userID    用户 userID
     * @param historyID 当前对话的 ID
     * @param queryStr  prompt
     * @return 流数据
     */
    @GetMapping(value = "/completions/stream/{userID}&{historyID}&{queryStr}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamCompletions(@PathVariable Long userID, @PathVariable Long historyID, @PathVariable String queryStr) throws UnsupportedEncodingException {
        byte[] decodeQueryStr = Base64.getDecoder().decode(queryStr);
        return gptService.send(URLDecoder.decode(new String(decodeQueryStr), StandardCharsets.UTF_8), userID, historyID);
    }

    /**
     * chatGPT 长对话接口, 构造 JSON 数据形式, 1.0
     *
     * @param chatCplQueryReq 长对话查询对象
     * @return 长对话返回对象
     */
    @Deprecated
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

    /**
     * chatGPT 长对话接口, 使用第三方依赖, 2.0
     *
     * @param chatCplQueryReq 长对话查询对象
     * @return 长对话返回对象
     */
    @Deprecated
    @PostMapping("/chatCompletion2")
    @ResponseBody
    public CommonResp<ChatCplQueryResp> chatCompletion2(@RequestBody @Valid ChatCplQueryReq chatCplQueryReq) {
        CommonResp<ChatCplQueryResp> resp = new CommonResp<>();
        ChatCplQueryResp res = gptService.chatCompletion2(chatCplQueryReq);
        if (res == null) {
            resp.setSuccess(false);
            resp.setMessage("接口超时, 请重试");
        }
        resp.setContent(res);
        return resp;
    }

    /**
     * chatGPT 图片生成接口
     *
     * @param prompt 描述图片 prompt
     * @return GPT 生成的临时图片的 URL
     */
    @PostMapping("/image/{prompt}")
    @ResponseBody
    public CommonResp<String> image(@PathVariable String prompt) {
        CommonResp<String> resp = new CommonResp<>();
        String res = gptService.image(URLDecoder.decode(prompt, StandardCharsets.UTF_8));
        if (res == null) {
            resp.setSuccess(false);
            resp.setMessage("接口超时, 请重试");
        }
        resp.setContent(res);
        return resp;
    }

    /**
     * 查询某个用户下的所有历史记录
     *
     * @param userID 用户 ID
     * @return 普通返回
     */
    @GetMapping("/selectAllByID/{userID}")
    @ResponseBody
    public CommonResp selectAll(@PathVariable Long userID) {
        CommonResp<List<ChatHistory>> resp = new CommonResp<>();

        resp.setContent(gptService.selectAllByID(userID));

        return resp;
    }

    /**
     * 根据某个长对话的 historyID 查找其内容
     *
     * @param historyId 长对话的 historyID
     * @return 长对话的内容
     */
    @GetMapping("/selectContentByID/{historyId}")
    @ResponseBody
    public CommonResp<String> selectContentByID(@PathVariable Long historyId) {
        CommonResp<String> resp = new CommonResp<>();
        resp.setContent(gptService.selectContentByID(historyId));
        return resp;
    }
}
