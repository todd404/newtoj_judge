package com.toddwu.toj_judge.mq_sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toddwu.toj_judge.pojo.judge.JudgeReport;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class JudgeReportMQSender {
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void send(String uuid){
        rabbitTemplate.convertAndSend("judgeReportQueue", uuid);
    }
}
