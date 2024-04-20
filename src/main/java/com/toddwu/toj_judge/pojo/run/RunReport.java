package com.toddwu.toj_judge.pojo.run;

import lombok.Data;

@Data
public class RunReport {
    Integer statusCode;
    String msg;
    RunConfig runConfig;
}
