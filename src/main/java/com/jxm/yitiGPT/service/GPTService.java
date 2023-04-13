package com.jxm.yitiGPT.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jxm.yitiGPT.Client.OpenAiWebClient;
import com.jxm.yitiGPT.domain.ChatHistory;
import com.jxm.yitiGPT.domain.ChatHistoryContent;
import com.jxm.yitiGPT.domain.ChatHistoryExample;
import com.jxm.yitiGPT.domain.MessageText;
import com.jxm.yitiGPT.enmus.MessageType;
import com.jxm.yitiGPT.enmus.UserType;
import com.jxm.yitiGPT.listener.CompletedCallBack;
import com.jxm.yitiGPT.listener.OpenAISubscriber;
import com.jxm.yitiGPT.mapper.ChatHistoryContentMapper;
import com.jxm.yitiGPT.mapper.ChatHistoryMapper;
import com.jxm.yitiGPT.req.ChatCplQueryReq;
import com.jxm.yitiGPT.resp.ChatCplQueryResp;
import com.jxm.yitiGPT.resp.Message;
import com.jxm.yitiGPT.utils.SnowFlakeIdWorker;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GPTService implements CompletedCallBack {

    @Resource
    private ChatHistoryMapper chatHistoryMapper;

    @Resource
    private ChatHistoryContentMapper chatHistoryContentMapper;

    @Resource
    private SnowFlakeIdWorker snowFlakeIdWorker;

    private static final Logger LOG = LoggerFactory.getLogger(GPTService.class);

    private final String[] OPENAI_TOKEN = new String[] {
            "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",
            "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",
            "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",
            "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",
            "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",
            "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",
            "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",
            "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",
            "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",
            "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",
            "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"
    };

    private final OpenAiWebClient openAiWebClient;

    @Value("${my.openai.key}")
    private String API_KEY;


    public Flux<String> send(MessageType type, String content) {


        Message userMessage = new Message(MessageType.TEXT, UserType.USER, content);


        LOG.info("prompt:{}", content);
        return Flux.create(emitter -> {
            OpenAISubscriber subscriber = new OpenAISubscriber(emitter, API_KEY, this, userMessage);
            Flux<String> openAiResponse =
                    openAiWebClient.getChatResponse(API_KEY, content, null, null, null);
            openAiResponse.subscribe(subscriber);
            emitter.onDispose(subscriber);
        });
    }

    @Override
    public void completed(Message questions, String sessionId, String response) {
        // mysql
    }

    @Override
    public void fail(String sessionId) {
        // fail
    }


    public String sendPost2(String data) {

        String OPENAI_TOKEN = "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";
        // 构建openai api对象，由于处理时间比较长，建议设置一个合理的超时时间
        OpenAiService service = new OpenAiService(OPENAI_TOKEN, Duration.ofSeconds(60));
        CompletionRequest completionRequest = CompletionRequest.builder()
                .prompt("如果有一辆车，车里面坐了小明小红小黄，请问车是谁的？")
                .model("text-davinci-003")
                .temperature(0.9d)
                .maxTokens(150)
                .stop(Arrays.asList("Human:", "AI:"))
                .echo(true)
                .build();
        service.createCompletion(completionRequest).getChoices().forEach(System.out::println);

        return null;
    }

    /**
     * 长对话 1.0
     */
    public ChatCplQueryResp chatCompletion(ChatCplQueryReq chatCplQueryReq) {
        ChatCplQueryResp chatCplQueryResp = null;
        LOG.info(chatCplQueryReq.toString());
        String resContent = "";
        String queryStr = chatCplQueryReq.getQueryStr();


        ChatHistory historyMes = new ChatHistory();                                 // 历史记录 obj
        ChatHistoryContent historyMesContent = new ChatHistoryContent("");          // 历史记录内容 obj
        if (chatCplQueryReq.getHistoryID() != -1) {                                 // 获取本次对话的历史记录
            historyMes = chatHistoryMapper.selectByPrimaryKey(chatCplQueryReq.getHistoryID());
            historyMesContent = chatHistoryContentMapper.selectByPrimaryKey(historyMes.getContentId());
            LOG.info(historyMes.toString());
            LOG.info(historyMesContent.toString());
        }

        // 设置请求体
        RestTemplate client = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization","Bearer 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");
        httpHeaders.add("Content-Type", "application/json");  // 传递请求体时必须设置

        // 在 content 字段中, 添加本次对话的历史记录
        String requestJson = String.format(
                "{\n" +
                        "    \"model\": \"gpt-3.5-turbo-0301\",\n" +
                        "    \"messages\":" +
                        "[" + historyMesContent.getContent() + "{\"role\": \"user\", \"content\": %s}],\n" +
                        "    \"temperature\": 0, \n" +
                        "    \"max_tokens\": 2048\n" +
                        "}", queryStr
        );

        LOG.info(requestJson);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson,httpHeaders);
        ResponseEntity<String> response = client.exchange("https://api.openai.com/v1/chat/completions", HttpMethod.POST, entity, String.class);
//        System.out.println(response.getBody());
        JSONObject jsonObject = JSONObject.parseObject(response.getBody());
        if (jsonObject != null) {
            resContent = jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

            // 下面更新本次对话的历史记录
            LOG.info(historyMes.toString());
            if (chatCplQueryReq.getHistoryID() == -1) {                                       // 此次对话为新对话
                historyMesContent = new ChatHistoryContent();                                 // 无论是新旧对话, 都要更新 content
                historyMesContent.setId(snowFlakeIdWorker.nextId());
                historyMesContent.setContent(String.format("{\"role\": \"user\", \"content\": %s}", JSON.toJSONString(queryStr)) +
                        String.format(",{\"role\": \"assistant\", \"content\": %s},", JSON.toJSONString(resContent)));
                LOG.info(historyMesContent.toString());
                chatHistoryContentMapper.insert(historyMesContent);

                queryStr = queryStr.length() > 50 ? queryStr.substring(0, 50) : queryStr;    // title 的长度限制在 50
                historyMes.setId(snowFlakeIdWorker.nextId());                                // 设置 id
                historyMes.setUserId(chatCplQueryReq.getUserID());                           // 设置历史记录所属 user
                historyMes.setTitle(queryStr);                                               // 设置这次对话的 title
                historyMes.setContentId(historyMesContent.getId());                          // 设置历史记录内容 id

                LOG.info(historyMes.toString());
                chatHistoryMapper.insert(historyMes);
            } else {                                                                         // 此次对话为旧对话, 只用更新 content
                historyMesContent.setContent(historyMesContent.getContent() +
                        String.format("{\"role\": \"user\", \"content\": %s}", JSON.toJSONString(queryStr)) +
                        String.format(",{\"role\": \"assistant\", \"content\": %s},", JSON.toJSONString(resContent)));
                chatHistoryContentMapper.updateByPrimaryKeyWithBLOBs(historyMesContent);
                LOG.info("lala" + historyMesContent.toString());
            }
            chatCplQueryResp = new ChatCplQueryResp();
            chatCplQueryResp.setContent(resContent);
            chatCplQueryResp.setHistoryID(historyMes.getId());

        }
        return chatCplQueryResp;
    }

    /**
     * 长对话 2.0
     * 使用 jar 包
     */
    public ChatCplQueryResp chatCompletion2(ChatCplQueryReq chatCplQueryReq) {
        if (chatCplQueryReq.getHistoryID() == -1) {
            return chatCompletionFirst(chatCplQueryReq);
        }
        OpenAiService service = new OpenAiService(OPENAI_TOKEN[(int) (Math.random() * OPENAI_TOKEN.length)], Duration.ofSeconds(60));
        ChatCplQueryResp resp = new ChatCplQueryResp();

        // 获取本次对话的历史记录和内容
        ChatHistory historyMes = chatHistoryMapper.selectByPrimaryKey(chatCplQueryReq.getHistoryID());                    // 历史记录 obj
        ChatHistoryContent historyMesContent = chatHistoryContentMapper.selectByPrimaryKey(historyMes.getContentId());    // 历史记录内容 obj
        List<ChatMessage> historyList = JSON.parseArray(historyMesContent.getContent(), ChatMessage.class);               // 将其反序列化出来


        ArrayList<ChatMessage> chatMessages = new ArrayList<>(historyList);
        chatMessages.add(new ChatMessage(ChatMessageRole.USER.value(), chatCplQueryReq.getQueryStr()));

        String resContent = null;
        try {
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                    .builder()
                    .model("gpt-3.5-turbo")
                    .messages(chatMessages)
                    .n(1)
                    .maxTokens(2048)
//                    .logitBias(new HashMap<>())
                    .build();
            resContent = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage().getContent();  // GPT 的回复

        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }


        // 接下来拼装新的历史记录
        chatMessages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), resContent));
        // 旧对话只需改历史记录内容表即可
        historyMesContent.setContent(JSON.toJSONString(chatMessages));
        chatHistoryContentMapper.updateByPrimaryKeyWithBLOBs(historyMesContent);

        service.shutdownExecutor();

        resp.setHistoryID(historyMes.getId());
        resp.setContent(resContent);

        return resp;
    }

    /**
     * 首次对话
     */
    private ChatCplQueryResp chatCompletionFirst(ChatCplQueryReq chatCplQueryReq) {
        ChatCplQueryResp resp = new ChatCplQueryResp();
        OpenAiService service = new OpenAiService(OPENAI_TOKEN[(int) (Math.random() * OPENAI_TOKEN.length)], Duration.ofSeconds(60));

        // 拼装问题
        ArrayList<ChatMessage> chatMessages = new ArrayList<>() {{ add(new ChatMessage(ChatMessageRole.USER.value(), chatCplQueryReq.getQueryStr())); }};

        String resContent = null;
        try {
            ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                    .builder()
                    .model("gpt-3.5-turbo")
                    .messages(chatMessages)
                    .n(1)
                    .maxTokens(2048)
                    .build();
            resContent = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage().getContent();  // GPT 的回复

        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }

        // 接下来拼装历史记录
        chatMessages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), resContent));

        // 更新数据库
        // 先更新历史记录内容表
        ChatHistoryContent historyMesContent = new ChatHistoryContent();
        historyMesContent.setId(snowFlakeIdWorker.nextId());
        historyMesContent.setContent(JSON.toJSONString(chatMessages));
        chatHistoryContentMapper.insert(historyMesContent);

        // 再更新历史记录表
        String title = chatCplQueryReq.getQueryStr().length() > 50                  // title 的长度限制在 50
                ? chatCplQueryReq.getQueryStr().substring(0, 50)
                : chatCplQueryReq.getQueryStr();

        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setId(snowFlakeIdWorker.nextId());                              // 设置 id
        chatHistory.setUserId(chatCplQueryReq.getUserID());                         // 设置历史记录所属 user
        chatHistory.setTitle(title);                                                // 设置这次对话的 title
        chatHistory.setContentId(historyMesContent.getId());                        // 设置历史记录内容 id
        chatHistoryMapper.insert(chatHistory);

        service.shutdownExecutor();                                                 // 关闭连接

        resp.setHistoryID(chatHistory.getId());
        resp.setContent(resContent);

        return resp;
    }

    /**
     * 根据用户 ID 查找所有的历史记录
     */
    public List<ChatHistory> selectAllByID(Long userID) {
        List<ChatHistory> res = null;
        ChatHistoryExample chatHistoryExample = new ChatHistoryExample();
        ChatHistoryExample.Criteria criteria = chatHistoryExample.createCriteria();
        criteria.andUserIdEqualTo(userID);

        try {
            PageHelper.startPage(1, 20, true);
            res = chatHistoryMapper.selectByExample(chatHistoryExample);
            PageInfo<ChatHistory> downloadListPageInfo = new PageInfo<>(res);
            LOG.info("当前页: " + downloadListPageInfo.getPageNum()
                    + ", 总页数: " + downloadListPageInfo.getPages()
                    + " , 总记录数: " + downloadListPageInfo.getTotal());
        } catch (Exception e) {
            e.printStackTrace();
        }


        return res;
    }

    /**
     * 根据历史记录 ID 查找对话内容
     */
    public String selectContentByID(Long historyId) {
        ChatHistory chatHistory = chatHistoryMapper.selectByPrimaryKey(historyId);
        ChatHistoryContent chatHistoryContent = chatHistoryContentMapper.selectByPrimaryKey(chatHistory.getContentId());
        return chatHistoryContent.getContent();
    }

    public String image(String prompt) {
        OpenAiService service = new OpenAiService(OPENAI_TOKEN[(int) (Math.random() * OPENAI_TOKEN.length)], Duration.ofSeconds(60));

        ImageResult image = null;
        try {
            CreateImageRequest request = CreateImageRequest.builder()
                    .prompt(JSON.toJSONString(prompt))
                    .build();

            image = service.createImage(request);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }
        return image.getData().get(0).getUrl();
    }

}
