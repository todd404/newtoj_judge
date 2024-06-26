package com.toddwu.toj_judge.mq_listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toddwu.toj_judge.judge.CppJudge;
import com.toddwu.toj_judge.pojo.judge.JudgeConfig;
import com.toddwu.toj_judge.pojo.run.RunConfig;
import com.toddwu.toj_judge.run.CppRun;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "language-support.cpp-run", havingValue = "true")
public class CppRunMQListener {
    @Autowired
    private ObjectProvider<CppRun> cppRunObjectProvider;

    @RabbitListener(queues = "cppRunQueue", concurrency = "10")
    public void listenRunQueueMessage(String msg){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            RunConfig runConfig = objectMapper.readValue(msg, RunConfig.class);
            CppRun cppRun = cppRunObjectProvider.getObject();
            cppRun.setRunConfig(runConfig);
            cppRun.runForResult();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
