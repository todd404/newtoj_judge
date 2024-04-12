package com.toddwu.toj_judge.pojo;

import lombok.Data;

@Data
public class JudgeConfig {
    String language;
    String problemId;
    String uuid;
    String code;
    ProblemConfig problemConfig;
}
