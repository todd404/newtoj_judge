package com.toddwu.toj_judge.pojo;

import com.toddwu.toj_judge.pojo.judge.ScoreConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProblemConfig implements Serializable {
    String functionName;
    String returnType;
    List<String> argumentTypeList = new ArrayList<>();
    List<String> argumentNameList = new ArrayList<>();
    Integer timeLimit;
    Integer memoryLimit;
    ScoreConfig scoreConfig;
}
