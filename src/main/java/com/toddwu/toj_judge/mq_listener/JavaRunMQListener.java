package com.toddwu.toj_judge.mq_listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toddwu.toj_judge.pojo.run.RunConfig;
import com.toddwu.toj_judge.run.CppRun;
import com.toddwu.toj_judge.run.JavaRun;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "language-support.java-run", havingValue = "true")
public class JavaRunMQListener {
    @Autowired
    private ObjectProvider<JavaRun> javaRunObjectProvider;
    @RabbitListener(queues = "javaRunQueue")
    public void listenRunQueueMessage(String msg){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            RunConfig runConfig = objectMapper.readValue(msg, RunConfig.class);
            JavaRun javaRun = javaRunObjectProvider.getObject();
            javaRun.setRunConfig(runConfig);
            javaRun.runForResult();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
