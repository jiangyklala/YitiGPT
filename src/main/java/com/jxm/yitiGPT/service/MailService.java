package com.jxm.yitiGPT.service;

import jakarta.annotation.Resource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class MailService {

    //注入邮件发送对象
    @Resource
    private JavaMailSender mailSender;

    public boolean warnMeSend(String content, Integer level) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("jiangykmm@gmail.com");                         // 设置收件人邮箱  //"GPTalk<13837774652@163.com>"
        message.setFrom("GPTalk<13837774652@163.com>");
        message.setSubject("yitiGPT Warning Level: " + level);        // 设置邮件主题
        message.setText(content);                                     // 设置邮件文本内容
        message.setSentDate(new Date());                              // 设置邮件发送时间

        try {
            //发送
            mailSender.send(message);
        } catch (MailException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
