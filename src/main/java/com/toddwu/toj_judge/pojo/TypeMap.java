package com.toddwu.toj_judge.pojo;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Data
@ConfigurationProperties(prefix = "type-maps")
public class TypeMap {
    private Map<String, String> cppTypeMap;
    private Map<String, String> argumentFunctionMap;
    private Map<String, String> returnFunctionMap;
}
