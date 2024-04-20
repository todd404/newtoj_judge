package com.toddwu.toj_judge.pojo.judge;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class JudgeReport {
    Integer statusCode; //100,排队;101,编译中;102,判题中;200,全对;201,有错;500,超过限制;
    String msg = "";
    String memoryUsed;
    String timeUsed;
    Integer basicCasesCorrectLine = 0;
    Boolean basicCasesPassed = false;
    List<Boolean> specialCasesPassedList = new ArrayList<>();
    JudgeConfig judgeConfig;
}
