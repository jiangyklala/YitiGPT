package com.jxm.yitiGPT.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jxm.yitiGPT.Client.OpenAiWebClient;
import com.jxm.yitiGPT.domain.ChatHistory;
import com.jxm.yitiGPT.domain.ChatHistoryContent;
import com.jxm.yitiGPT.domain.ChatHistoryExample;
import com.jxm.yitiGPT.domain.User;
import com.jxm.yitiGPT.enmus.UserType;
import com.jxm.yitiGPT.listener.CompletedCallBack;
import com.jxm.yitiGPT.listener.OpenAISubscriber;
import com.jxm.yitiGPT.mapper.ChatHistoryContentMapper;
import com.jxm.yitiGPT.mapper.ChatHistoryMapper;
import com.jxm.yitiGPT.mapper.UserMapper;
import com.jxm.yitiGPT.mapper.cust.UserMapperCust;
import com.jxm.yitiGPT.req.ChatCplQueryReq;
import com.jxm.yitiGPT.resp.ChatCplQueryResp;
import com.jxm.yitiGPT.resp.CommonResp;
import com.jxm.yitiGPT.resp.Message;
import com.jxm.yitiGPT.utils.SnowFlakeIdWorker;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GPTService implements CompletedCallBack {

    public static final String[] OPENAI_TOKEN = new String[]{
            "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",
            "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",
    };
    private final OpenAiWebClient openAiWebClient;
    private static Encoding enc;
    public static JedisPool jedisPool;

    @Value("${my.redis.ip}")
    private String myRedisIP;

    @Resource
    private ChatHistoryMapper chatHistoryMapper;

    @Resource
    private ChatHistoryContentMapper chatHistoryContentMapper;

    @Resource
    private SnowFlakeIdWorker snowFlakeIdWorker;

    @Resource
    private UserMapperCust userMapperCust;

    @Resource
    private UserMapper userMapper;

    @PostConstruct
    public void init() {
        enc = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
        jedisPool = new JedisPool(setJedisPoolConfig(), myRedisIP, 6379, 5000, "jiang", 1);
    }

    /**
     * 配置连接池
     */
    public JedisPoolConfig setJedisPoolConfig() {
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(200);                                    // 最大连接对象
        config.setJmxEnabled(true);
        config.setMaxIdle(100);                                     // 最大闲置对象
        config.setMinIdle(100);                                     // 最小闲置对象
        config.setTestOnBorrow(true);                               // 向资源池借用连接时是否做有效性检测
        config.setTestOnReturn(true);                               // 向资源池归还连接时是否做有效性检测
        config.setTestWhileIdle(true);                              // 是否在空闲资源检测时通过 ping 命令检测连接的有效性,无效连接将被销毁
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(5));   // 空闲资源的检测周期
        config.setMaxWait(Duration.ofSeconds(5));                   // 当资源池连接用尽后，调用者的最大等待时间
        config.setMinEvictableIdleTime(Duration.ofSeconds(10));
        config.setBlockWhenExhausted(true);                         // 当获取不到连接时应阻塞

        return config;
    }

    /**
     * 发送提问信息
     *
     * @param queryStr  用户的问题
     * @param userID    userID
     * @param historyID 历史记录 ID
     * @return 包
     */
    public Flux<String> send(String queryStr, Long userID, Long historyID, Integer totalToken) {
        final String prompt;
        final List<Message> historyList;

        // 查询历史记录
        if (historyID != -1) {
            // 获取本次对话的历史记录和内容
            ChatHistory historyMes = chatHistoryMapper.selectByPrimaryKey(historyID);                                         // 历史记录 obj
            ChatHistoryContent historyMesContent = chatHistoryContentMapper.selectByPrimaryKey(historyMes.getContentId());    // 历史记录内容 obj
            historyList = JSON.parseArray(historyMesContent.getContent(), Message.class);                                     // 将其反序列化出来

            List<Message> historyListTmp = new ArrayList<>(historyList.subList(0, Math.min(historyList.size(), 2)));

            for (int i = 2; i < historyList.size(); ++i) {
                if (historyList.get(i).getIfUse()) {
                    historyListTmp.addAll(historyList.subList(i, historyList.size()));
//                    log.info("historyListTmp : {}", historyListTmp.toString());
                    break;
                }
            }

            String historyDialogue = historyListTmp.stream().map(e -> String.format(e.getUserType().getCode(), e.getMessage())).collect(Collectors.joining());

            // 将问题格式化
            prompt = String.format("%s\nA: ", historyDialogue);

//            log.info("prompt: {}", prompt);
        } else {
            prompt = queryStr;
            historyList = null;
        }

        Message userMessage = new Message(UserType.USER, queryStr);

//        log.info("message:{}", userMessage);
        return Flux.create(emitter -> {
            OpenAISubscriber subscriber = new OpenAISubscriber(emitter, this, userMessage, userID, historyID, totalToken, historyList);
            Flux<String> openAiResponse =
                    openAiWebClient.getChatResponse(OPENAI_TOKEN[(int) (Math.random() * OPENAI_TOKEN.length)], prompt, 1024, null, null);
            openAiResponse.subscribe(subscriber);
            emitter.onDispose(subscriber);
        });
    }


    @Override
    public void completedFirst(Message questions, String answer, Long userID, Long historyID) {
        Message botMessage = new Message(UserType.BOT, answer);
        // 先更新历史记录内容表
        ChatHistoryContent historyMesContent = new ChatHistoryContent();
        historyMesContent.setId(snowFlakeIdWorker.nextId());
        historyMesContent.setContent(JSON.toJSONString(new ArrayList<>(2) {{
            add(questions);
            add(botMessage);
        }}));
        chatHistoryContentMapper.insert(historyMesContent);
//        log.info(historyMesContent.toString());
        // 再更新历史记录表
        String title = questions.getMessage().length() > 50                         // title 的长度限制在 50
                ? questions.getMessage().substring(0, 50)
                : questions.getMessage();

        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setId(historyID);                                               // 设置 id
        chatHistory.setUserId(userID);                                              // 设置历史记录所属 user
        chatHistory.setTitle(title);                                                // 设置这次对话的 title
        chatHistory.setContentId(historyMesContent.getId());                        // 设置历史记录内容 id
        chatHistoryMapper.insert(chatHistory);
//        log.info(chatHistory.toString());
    }

    @Override
    public void completed(String answer, Long userID, Long historyID, List<Message> historyList) {
        Message botMessage = new Message(UserType.BOT, answer);
        // 旧对话只需改历史记录内容表即可
        ChatHistory historyMes = chatHistoryMapper.selectByPrimaryKey(historyID);                    // 历史记录 obj
        historyList.add(botMessage);
        ChatHistoryContent historyMesContent = new ChatHistoryContent();
        historyMesContent.setId(historyMes.getContentId());
        historyMesContent.setContent(JSON.toJSONString(historyList));
        chatHistoryContentMapper.updateByPrimaryKeyWithBLOBs(historyMesContent);
//        log.info("只更改记录内容: {}", historyMesContent.toString());
    }

    @Override
    public void recordCost(Long userID, Integer totalToken, String response, List<Message> historyList) {
        // 计算本次对话的答案, 消耗的 tokens
        totalToken += enc.countTokens(response) + 21;

        // 扣除次数
        // token 超过500，扣除一次，其他逻辑不变。另外，大于300小于500也+1，但是小于300不扣除。
        long finalConsume = (totalToken / 500 == 0) ? 1 : totalToken / 500;
        if (totalToken % 500 > 300) {
            ++finalConsume;
        }

        try {
            userMapperCust.balanceGetAndDecrNum(userID, finalConsume);
        } catch (RuntimeException e) {
            log.error("权限验证(扣除提问次数)出错");
        }

        // 获取当日日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String nowTime = sdf.format(new Date());

        try (Jedis jedis = jedisPool.getResource()) {
            // 记录当日问题的总次数
            jedis.incr("yt:gpt:times:" + nowTime);

            // 记录当日消耗的总 tokens
            jedis.incrBy("yt:gpt:tokens:" + nowTime, totalToken);
        } catch (Exception e) {
            log.error("更新 GPT 提问次数以及消耗token数失败");
        }

        log.info("每日提问信息记录完成, 时间: {}, userID: {}, 消耗提问次数: {}, 消耗 token 数: {}", nowTime, userID, finalConsume, totalToken);

    }

    @Override
    public void fail(String sessionId) {
        // fail
    }

    @Deprecated
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
    @Deprecated
    public ChatCplQueryResp chatCompletion(ChatCplQueryReq chatCplQueryReq) {
        ChatCplQueryResp chatCplQueryResp = null;
        log.info(chatCplQueryReq.toString());
        String resContent = "";
        String queryStr = chatCplQueryReq.getQueryStr();


        ChatHistory historyMes = new ChatHistory();                                 // 历史记录 obj
        ChatHistoryContent historyMesContent = new ChatHistoryContent("");          // 历史记录内容 obj
        if (chatCplQueryReq.getHistoryID() != -1) {                                 // 获取本次对话的历史记录
            historyMes = chatHistoryMapper.selectByPrimaryKey(chatCplQueryReq.getHistoryID());
            historyMesContent = chatHistoryContentMapper.selectByPrimaryKey(historyMes.getContentId());
            log.info(historyMes.toString());
            log.info(historyMesContent.toString());
        }

        // 设置请求体
        RestTemplate client = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer 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");
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

        log.info(requestJson);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson, httpHeaders);
        ResponseEntity<String> response = client.exchange("https://api.openai.com/v1/chat/completions", HttpMethod.POST, entity, String.class);
//        System.out.println(response.getBody());
        JSONObject jsonObject = JSONObject.parseObject(response.getBody());
        if (jsonObject != null) {
            resContent = jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

            // 下面更新本次对话的历史记录
            log.info(historyMes.toString());
            if (chatCplQueryReq.getHistoryID() == -1) {                                       // 此次对话为新对话
                historyMesContent = new ChatHistoryContent();                                 // 无论是新旧对话, 都要更新 content
                historyMesContent.setId(snowFlakeIdWorker.nextId());
                historyMesContent.setContent(String.format("{\"role\": \"user\", \"content\": %s}", JSON.toJSONString(queryStr)) +
                        String.format(",{\"role\": \"assistant\", \"content\": %s},", JSON.toJSONString(resContent)));
                log.info(historyMesContent.toString());
                chatHistoryContentMapper.insert(historyMesContent);

                queryStr = queryStr.length() > 50 ? queryStr.substring(0, 50) : queryStr;    // title 的长度限制在 50
                historyMes.setId(snowFlakeIdWorker.nextId());                                // 设置 id
                historyMes.setUserId(chatCplQueryReq.getUserID());                           // 设置历史记录所属 user
                historyMes.setTitle(queryStr);                                               // 设置这次对话的 title
                historyMes.setContentId(historyMesContent.getId());                          // 设置历史记录内容 id

                log.info(historyMes.toString());
                chatHistoryMapper.insert(historyMes);
            } else {                                                                         // 此次对话为旧对话, 只用更新 content
                historyMesContent.setContent(historyMesContent.getContent() +
                        String.format("{\"role\": \"user\", \"content\": %s}", JSON.toJSONString(queryStr)) +
                        String.format(",{\"role\": \"assistant\", \"content\": %s},", JSON.toJSONString(resContent)));
                chatHistoryContentMapper.updateByPrimaryKeyWithBLOBs(historyMesContent);
                log.info("lala" + historyMesContent.toString());
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
    @Deprecated
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
     * 首次对话提问 1.0
     */
    @Deprecated
    private ChatCplQueryResp chatCompletionFirst(ChatCplQueryReq chatCplQueryReq) {
        ChatCplQueryResp resp = new ChatCplQueryResp();
        OpenAiService service = new OpenAiService(OPENAI_TOKEN[(int) (Math.random() * OPENAI_TOKEN.length)], Duration.ofSeconds(60));

        // 拼装问题
        ArrayList<ChatMessage> chatMessages = new ArrayList<>() {{
            add(new ChatMessage(ChatMessageRole.USER.value(), chatCplQueryReq.getQueryStr()));
        }};

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
            log.info("当前页: " + downloadListPageInfo.getPageNum()
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

    /**
     * 生成 image 接口
     *
     * @param prompt 用户的描述
     * @return 生成图片的 URL
     */
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

    /**
     * 提问次数消耗
     *
     * @param userID    userID
     * @param historyID 历史记录 ID
     * @param resp      回调 resp
     */
    public void payForAns(Long userID, Long historyID, String queryStr, CommonResp<Integer> resp) {
        int finalToken = 0;                     // 当次对话的, 最终提问需要消耗的总 token
        int tokenOffset = 5;
        List<Message> historyList = null;
        Message queryStrMessage = new Message(UserType.USER, queryStr, true);

        if (enc.countTokens(queryStrMessage.getMessage()) > 3000) {
            resp.setMessage("提问内容过长!! 减少一点呗");
            resp.setSuccess(false);
            return;
        } else {
            finalToken += enc.countTokens(queryStrMessage.getMessage()) + tokenOffset;
        }

        if (historyID != -1) {
            // 查询历史记录
            // 获取本次对话的历史记录和内容
            ChatHistory historyMes = chatHistoryMapper.selectByPrimaryKey(historyID);                                         // 历史记录 obj
            ChatHistoryContent historyMesContent = chatHistoryContentMapper.selectByPrimaryKey(historyMes.getContentId());    // 历史记录内容 obj
            historyList = JSON.parseArray(historyMesContent.getContent(), Message.class);                                     // 将其反序列化出来

            // 第一次对话, 肯定需要 (如果可以的话)
            if (finalToken < 3000) {
                Message messageUser = historyList.get(0);
                Message messageAssistant = historyList.get(1);

                finalToken += enc.countTokens(messageUser.getMessage()) + tokenOffset
                        + enc.countTokens(messageAssistant.getMessage()) + tokenOffset;
            } else {
                resp.setMessage("本次连续对话内容过长了呦, 开启一个新对话呗");
                resp.setSuccess(false);
                return;
            }


            // 之后倒着往上截取, 每两个 Message 为一段对话
            boolean stopFlag = false;
            for (int i = historyList.size() - 1; i >= 2; --i) {
                Message messageUser = historyList.get(i);
                Message messageAssistant = historyList.get(--i);
                if (stopFlag) {
                    messageUser.setIfUse(false);
                    messageAssistant.setIfUse(false);
                    continue;
                }
                int token = enc.countTokens(messageUser.getMessage()) + tokenOffset
                        + enc.countTokens(messageAssistant.getMessage()) + tokenOffset;

                if (token + finalToken > 2900) {
//                    log.info("too big! i = {}, token = {}, finalToken = {}", i, token, finalToken);
                    stopFlag = true;
                    // 留 4096 - 1024 - 2900 = 172 用于别的消耗 (user_type 之类的)
                    messageUser.setIfUse(false);
                    messageAssistant.setIfUse(false);
                } else {
                    // 给需要用作本次对话的上下文做标记
                    messageUser.setIfUse(true);
                    messageAssistant.setIfUse(true);

                    finalToken += token;
                }
            }

            // 将最后一次提问加入
            historyList.add(queryStrMessage);

            // 更改用户历史记录
            try {
                historyMesContent.setContent(JSON.toJSONString(historyList));
                chatHistoryContentMapper.updateByPrimaryKeyWithBLOBs(historyMesContent);
            } catch (RuntimeException e) {
                resp.setSuccess(false);
                resp.setMessage("更新历史记录表失败");
                log.error("更新 history_content 表失败", e);
                return;
            }

        }
        log.info("用户ID: {}, 提问消耗的token: {}, 是否是新对话: {}", userID, finalToken, historyID == -1);

        long finalConsume = (finalToken / 500 == 0) ? 1 : finalToken / 500;
        if (finalToken % 500 > 300) {
            ++finalConsume;
        }
        try {
            User user = userMapper.selectByPrimaryKey(userID);  // 查的是整个 user, 性能可提升
            if (user.getBalance() < finalConsume) {
                log.info("剩余提问次数不足:{}, 需要:{}", user.getBalance(), finalConsume);
                resp.setSuccess(false);
                resp.setMessage("剩余提问次数不足");
                return;
            }
        } catch (RuntimeException e) {
            resp.setSuccess(false);
            resp.setMessage("用户权限验证出错");
            log.error("权限验证(扣除提问次数)出错");
        }


        // 设置当前 (所有历史记录和) 提问消耗的 token
        resp.setContent(finalToken);


    }
}
