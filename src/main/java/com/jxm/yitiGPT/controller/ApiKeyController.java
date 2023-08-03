package com.jxm.yitiGPT.controller;

import com.jxm.yitiGPT.service.ApiKeyService;
import com.jxm.yitiGPT.service.GPTService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

@Controller
@RequestMapping("/key")
public class ApiKeyController {

    @Resource
    private ApiKeyService apiKeyService;

    @PostMapping("/addKey")
    @ResponseBody
    public String addKey(String key) {
        return apiKeyService.addKey(key);
    }

    @GetMapping("/srandKey")
    @ResponseBody
    public String srandKey() {
        return apiKeyService.srandKey();
    }

    @GetMapping("/allKeys")
    @ResponseBody
    public String allKeys() {
        return apiKeyService.allKeys();
    }

    @GetMapping("/delAKey")
    @ResponseBody
    public String delAKey(String key) {
        return apiKeyService.delAKey(key);
    }
}
