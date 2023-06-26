package com.jxm.yitiGPT.Client;

import com.alibaba.fastjson2.JSONObject;
import com.jxm.yitiGPT.Constant.GPTConstant;
import com.jxm.yitiGPT.service.GPTService;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.SSLException;
import java.util.Collections;

import static com.jxm.yitiGPT.service.GPTService.OPENAI_TOKEN;

@Slf4j
@Component
public class OpenAiWebClient {
    private WebClient webClient;

    @Value("${my.openai.env}")
    private String ENV;


    /**
     * dev 采用代理访问
     */
    @PostConstruct
    public void init() {
        log.info("init:{}", ENV);
        if (ENV.contains("test")) {
            initDev();
        } else {
            initProd();
        }
    }

    public void initDev() {
        log.info("initDev");
        SslContext sslContext = null;
        try {
            sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }

        // 创建HttpClient对象，并设置代理
        SslContext finalSslContext = sslContext;
        HttpClient httpClient = HttpClient.create()
                .secure(sslContextSpec -> sslContextSpec.sslContext(finalSslContext))
                .tcpConfiguration(tcpClient -> tcpClient.proxy(proxy ->
                        proxy.type(ProxyProvider.Proxy.HTTP).host("127.0.0.1").port(7890)));

        //海外正式不需要代理
        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        this.webClient = WebClient.builder().clientConnector(connector)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }

    /**
     * 生产环境初始化
     */
    public void initProd() {
        log.info("initProd");
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }


    /**
     * 获取连续对话的回答
     *
     * @param authorization API_KEY
     * @param queryStr      问题
     * @param maxTokens     最大 token 限制
     * @param temperature   temperature
     * @param topP          topP
     * @return 流返回
     */
    public Flux<String> getChatResponse(String authorization, String queryStr, Integer maxTokens, Double temperature, Double topP) {
        JSONObject params = new JSONObject();

        params.put("model", "gpt-3.5-turbo");
        params.put("max_tokens", maxTokens);
        params.put("stream", true);
        params.put("temperature", temperature);
        params.put("top_p", topP);
//        params.put("user", user);
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", queryStr);
        params.put("messages", Collections.singleton(message));

//        log.info("API_KEY = {}", authorization);

        return webClient.post()
                .uri(GPTConstant.CHAT_API)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorization)
                .bodyValue(params.toJSONString())
                .retrieve()
                .bodyToFlux(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    HttpStatusCode status = ex.getStatusCode();
                    String res = ex.getResponseBodyAsString();
                    log.error("API_KEY = {} \n" +
                            "OpenAI API error: {} {}", authorization, status, res);
                    return Mono.error(new RuntimeException(res));
                });

    }

    /**
     * 检查内容
     *
     * @param prompt 问题
     */
    public Mono<ServerResponse> checkContent(String prompt) {
        JSONObject params = new JSONObject();
        params.put("input", prompt);
        return webClient.post()
                .uri(GPTConstant.CONTENT_AUDIT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + OPENAI_TOKEN[(int) (Math.random() * OPENAI_TOKEN.length)])
                .bodyValue(params.toJSONString())
                .retrieve()
                .bodyToMono(JSONObject.class)
                .flatMap(jsonObject -> {
                    // 在这里处理 JSON 对象，例如将其转换为其他类型
                    // 并将结果包装为响应体返回
                    log.info(jsonObject.toJSONString());
                    Boolean aBoolean = jsonObject.getJSONArray("results").getJSONObject(0).getBoolean("flagged");
                    return ServerResponse.ok()
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(BodyInserters.fromValue(aBoolean));
                });
    }
}
