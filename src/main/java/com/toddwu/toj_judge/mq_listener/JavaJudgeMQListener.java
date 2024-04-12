package com.toddwu.toj_judge.mq_listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toddwu.toj_judge.pojo.JudgeConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "language-support.java", havingValue = "true")
public class JavaJudgeMQListener {

    @RabbitListener(queues = "javaJudgeQueue")
    public void listenJudgeQueueMessage(String msg){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JudgeConfig judgeConfig = objectMapper.readValue(msg, JudgeConfig.class);
            return;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
