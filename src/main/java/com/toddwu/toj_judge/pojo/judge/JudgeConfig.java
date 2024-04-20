package com.toddwu.toj_judge.pojo.judge;

import com.toddwu.toj_judge.pojo.ProblemConfig;
import lombok.Data;

@Data
public class JudgeConfig {
    String language;
    String problemId;
    String uuid;
    String code;
    String type;
    String forUUID;
    ProblemConfig problemConfig;
}
