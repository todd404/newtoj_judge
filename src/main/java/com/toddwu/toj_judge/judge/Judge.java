package com.toddwu.toj_judge.judge;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toddwu.toj_judge.exception.RunningException;
import com.toddwu.toj_judge.mq_sender.JudgeReportMQSender;
import com.toddwu.toj_judge.pojo.judge.JudgeConfig;
import com.toddwu.toj_judge.pojo.judge.JudgeReport;
import com.toddwu.toj_judge.utils.MyUtils;
import com.toddwu.toj_judge.utils.RedisCache;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@Component
@Data
@Scope("prototype")
public abstract class Judge {
    @Autowired
    RedisCache redisCache;

    @Autowired
    JudgeReportMQSender judgeReportMQSender;

    JudgeConfig judgeConfig;

    File templateFile, testFile, answerFile, codeFile , uuidDir;

    void initFiles() throws IOException {
        templateFile = new File(judgeConfig.getUuid() + "/template." + judgeConfig.getLanguage());
        testFile = new File(judgeConfig.getUuid() + "/test.txt");
        answerFile = new File(judgeConfig.getUuid() + "/answer.txt");
        codeFile = new File(judgeConfig.getUuid() + "/Main." + judgeConfig.getLanguage());

        createUuidDir();
        downloadFiles();
        buildRealCodeFile();
    }

    Boolean createUuidDir(){
        uuidDir = new File(judgeConfig.getUuid());
        return uuidDir.mkdir();
    }

    void downloadFiles() throws IOException {
        String basePath = "/mnt/d/toj_files";
        String templateFileSrc = basePath + "/"
                + judgeConfig.getLanguage() + "/template/"
                + judgeConfig.getProblemId() + "." + judgeConfig.getLanguage();
        String testFileSrc = basePath + "/test/" + judgeConfig.getProblemId() + ".txt";
        String answerFileSrc = basePath + "/answer/" + judgeConfig.getProblemId() + ".txt";

        FileUtil.copy(Paths.get(templateFileSrc), templateFile.toPath());
        FileUtil.copy(Paths.get(testFileSrc), testFile.toPath());
        FileUtil.copy(Paths.get(answerFileSrc), answerFile.toPath());
    }

    void buildRealCodeFile() throws IOException {
        String templateCode = MyUtils.readFileToString(templateFile.getPath());
        String realCode = templateCode.replace("<code></code>", judgeConfig.getCode());

        Boolean err = codeFile.createNewFile();

        FileWriter fileWriter = new FileWriter(codeFile);
        fileWriter.write(realCode);
        fileWriter.close();
    }

    JudgeReport pullJudgeReport() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String msg = redisCache.getCacheObject("judge:" + judgeConfig.getUuid());

        return objectMapper.readValue(msg, JudgeReport.class);
    }

    void putJudgeReport(JudgeReport judgeReport) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        redisCache.setCacheObject("judge:" + judgeConfig.getUuid(), objectMapper.writeValueAsString(judgeReport), 10, TimeUnit.MINUTES);
    }

    void reportError(Integer code, String msg) throws JsonProcessingException {
        JudgeReport judgeReport = pullJudgeReport();
        judgeReport.setStatusCode(code);
        judgeReport.setMsg(msg);
        putJudgeReport(judgeReport);
    }

    abstract public void judgeCode();

    void run(ProcessBuilder processBuilder) throws Exception, RunningException {
        processBuilder.redirectInput(testFile);

        Process process = processBuilder.start();

        var waitResult = process.waitFor(Math.round(judgeConfig.getProblemConfig().getTimeLimit()), TimeUnit.SECONDS);
        if(!waitResult){
            //超时
            throw new RunningException("运行超时！");
        }

        //通过检查err流检查运行是否有错误
        //注意：time指令的输出也在错误流里，在最后一行，如果错误流一行也没有说明运行异常
        String errorLine;
        ArrayList<String> errorLineList = new ArrayList<>();
        while ((errorLine = process.errorReader().readLine()) != null){
            errorLineList.add(errorLine);
        }

        if(errorLineList.size() > 1){
            StringBuilder result = new StringBuilder();
            for(int i = 0; i < errorLineList.size() - 1; i++){
                result.append(errorLineList.get(i)).append("\n");
            }
            throw new RunningException(result.toString());
        }

        //获取time指令输出，判断是否超时或超内存
        String timeLine = errorLineList.get(errorLineList.size() - 1);
        if(timeLine != null){
            var splitTime = timeLine.split(",");
            double executeTime = Double.parseDouble(splitTime[0]);
            Integer memory = Integer.valueOf(splitTime[1]);

            if(executeTime > judgeConfig.getProblemConfig().getTimeLimit()){
                throw new RunningException("运行超时！");}

            if(memory > judgeConfig.getProblemConfig().getMemoryLimit()){
                throw new RunningException("运行超过内存限制！");
            }

            JudgeReport judgeReport = pullJudgeReport();
            judgeReport.setTimeUsed(String.valueOf(executeTime));
            judgeReport.setMemoryUsed(memory.toString());
            putJudgeReport(judgeReport);
        }else{
            throw new Exception("未知错误");
        }

        //判题
        BufferedReader inputReader = process.inputReader();
        Scanner answerScanner = new Scanner(answerFile);
        String resultLine;
        Integer BasicCasesCorrectCount = 0;
        List<Boolean> specialCasesPassedList = new ArrayList<>();
        int basicCasesCount = judgeConfig.getProblemConfig().getScoreConfig().getBasicCasesCount();
        while ((resultLine = inputReader.readLine()) != null){
            String trimResultLine = resultLine.trim();
            if(trimResultLine.isEmpty()) continue; //跳过空行防止意外

            if(answerScanner.hasNext()){

                String answerLine = answerScanner.nextLine().trim();
                if(trimResultLine.equals(answerLine)){
                    if(BasicCasesCorrectCount >= basicCasesCount){
                        specialCasesPassedList.add(true);
                    }else{
                        BasicCasesCorrectCount++;
                    }
                }else{
                    if(BasicCasesCorrectCount < basicCasesCount){
                        break;
                    }else{
                        specialCasesPassedList.add(false);
                    }
                }
            }
        }

        JudgeReport judgeReport = pullJudgeReport();
        if(answerScanner.hasNext()){
            judgeReport.setStatusCode(201);
        }else{
            judgeReport.setStatusCode(200);
        }

        judgeReport.setBasicCasesCorrectLine(BasicCasesCorrectCount);
        judgeReport.setBasicCasesPassed(!(BasicCasesCorrectCount < basicCasesCount));
        judgeReport.setSpecialCasesPassedList(specialCasesPassedList);
        judgeReport.setJudgeConfig(judgeConfig);
        putJudgeReport(judgeReport);
    }
}
