package com.jxm.yitiGPT.Client;

import com.alibaba.fastjson2.JSONObject;
import com.jxm.yitiGPT.Constant.GPTConstant;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.net.ssl.SSLException;
import java.util.Collections;

@Slf4j
@Component
public class OpenAiWebClient {
    private WebClient webClient;

    @Value("${my.openai.env}")
    private String ENV;


    /**
     * dev采用代理访问
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


    public void initProd() {
        log.info("initProd");
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }


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

        log.info("API_KEY = {}", authorization);

        return webClient.post()
                .uri(GPTConstant.CHAT_API)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorization)
                .bodyValue(params.toJSONString())
                .retrieve()
                .bodyToFlux(String.class)
                .onErrorResume(WebClientResponseException.class, ex -> {
                    HttpStatusCode status = ex.getStatusCode();
                    String res = ex.getResponseBodyAsString();
                    log.error("OpenAI API error: {} {}", status, res);
                    return Mono.error(new RuntimeException(res));
                });

    }

    public Mono<Boolean> checkContent(String authorization, String prompt) {
        JSONObject params = new JSONObject();
        params.put("input", prompt);
        Mono<JSONObject> toMono = webClient.post()
                .uri(GPTConstant.CONTENT_AUDIT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorization)
                .bodyValue(params.toJSONString())
                .retrieve()
                .bodyToMono(JSONObject.class);
        JSONObject jsonObject = toMono.block();
        return Mono.just(jsonObject.getJSONArray("results").getJSONObject(0).getBoolean("flagged"));
    }
}
