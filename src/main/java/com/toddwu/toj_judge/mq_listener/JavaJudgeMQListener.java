package com.toddwu.toj_judge.mq_listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toddwu.toj_judge.judge.CppJudge;
import com.toddwu.toj_judge.judge.JavaJudge;
import com.toddwu.toj_judge.pojo.judge.JudgeConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "language-support.java", havingValue = "true")
public class JavaJudgeMQListener {
    @Autowired
    private ObjectProvider<JavaJudge> javaJudgeObjectProvider;

    @RabbitListener(queues = "javaJudgeQueue", concurrency = "10")
    public void listenJudgeQueueMessage(String msg){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JudgeConfig judgeConfig = objectMapper.readValue(msg, JudgeConfig.class);
            JavaJudge javaJudge = javaJudgeObjectProvider.getObject();
            javaJudge.setJudgeConfig(judgeConfig);
            javaJudge.judgeCode();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
