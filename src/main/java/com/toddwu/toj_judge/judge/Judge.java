package com.toddwu.toj_judge.judge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toddwu.toj_judge.exception.RunningException;
import com.toddwu.toj_judge.pojo.JudgeConfig;
import com.toddwu.toj_judge.pojo.JudgeReport;
import com.toddwu.toj_judge.utils.MyUtils;
import com.toddwu.toj_judge.utils.RedisCache;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@Component
@Data
public abstract class Judge {
    @Autowired
    RedisCache redisCache;

    JudgeConfig judgeConfig;

    File templateFile, testFile, answerFile, codeFile , uuidDir;

    void initFiles() throws IOException {
        templateFile = new File(judgeConfig.getUuid() + "/template." + judgeConfig.getLanguage());
        testFile = new File(judgeConfig.getUuid() + "/test.txt");
        answerFile = new File(judgeConfig.getUuid() + "/answer.txt");
        codeFile = new File(judgeConfig.getUuid() + "/solution." + judgeConfig.getLanguage());

        createUuidDir();
        downloadFiles();
        buildRealCodeFile();
    }

    Boolean createUuidDir(){
        uuidDir = new File(judgeConfig.getUuid());
        return uuidDir.mkdir();
    }

    void downloadFiles() throws IOException {
        String downloadBasePath = "http://192.168.227.1/file";
        String templateFileSrc = downloadBasePath + "/"
                + judgeConfig.getLanguage() + "/template/"
                + judgeConfig.getProblemId() + "." + judgeConfig.getLanguage();
        String testFileSrc = downloadBasePath + "/test/" + judgeConfig.getProblemId() + ".txt";
        String answerFileSrc = downloadBasePath + "/answer/" + judgeConfig.getProblemId() + ".txt";


        MyUtils.downloadFile(templateFileSrc, templateFile.getPath());
        MyUtils.downloadFile(testFileSrc, testFile.getPath());
        MyUtils.downloadFile(answerFileSrc, answerFile.getPath());
    }

    void buildRealCodeFile() throws IOException {
        String templateCode = MyUtils.readFileToString(templateFile.getPath());
        String realCode = templateCode.replace("<code></code>", judgeConfig.getCode());

        Boolean err = codeFile.createNewFile();

        FileWriter fileWriter = new FileWriter(codeFile);
        fileWriter.write(realCode);
        fileWriter.close();
    }

    JudgeReport getJudgeReport() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String msg = redisCache.getCacheObject("judge:" + judgeConfig.getUuid());

        return objectMapper.readValue(msg, JudgeReport.class);
    }

    void setJudgeReport(JudgeReport judgeReport) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        redisCache.setCacheObject("judge:" + judgeConfig.getUuid(), objectMapper.writeValueAsString(judgeReport));
    }

    void reportError(Integer code, String msg) throws JsonProcessingException {
        JudgeReport judgeReport = getJudgeReport();
        judgeReport.setStatusCode(code);
        judgeReport.setMsg(msg);
        setJudgeReport(judgeReport);
    }

    abstract public void judgeCode();

    void run(ProcessBuilder processBuilder) throws Exception, RunningException {
        processBuilder.redirectInput(testFile);

        Process process = processBuilder.start();

        var waitResult = process.waitFor(Math.round(judgeConfig.getProblemConfig().getTimeLimit()), TimeUnit.MILLISECONDS);
        if(!waitResult){
            //超时
            throw new RunningException("运行超时！");
        }

        //TODO: 是否要加个返回值是否为0的判断？是否与下面的错误检查重复？

        //通过检查err流检查运行是否有错误
        //注意：time指令的输出也在错误流里，在最后一行，如果错误流一行也没有说明运行异常
        String errorLine ;
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

            JudgeReport judgeReport = getJudgeReport();
            judgeReport.setTimeUsed(String.valueOf(executeTime));
            judgeReport.setMemoryUsed(memory.toString());
            setJudgeReport(judgeReport);
        }else{
            throw new Exception("未知错误");
        }

        //判题
        BufferedReader inputReader = process.inputReader();
        Scanner answerScanner = new Scanner(answerFile);
        String resultLine;
        Integer BasicCasesCorrectCount = 0;
        Boolean basicCasesPassed = false;
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
                        basicCasesPassed = false;
                        break;
                    }else{
                        basicCasesPassed = true;
                        specialCasesPassedList.add(false);
                    }
                }
            }
        }

        JudgeReport judgeReport = getJudgeReport();
        if(answerScanner.hasNext()){
            judgeReport.setStatusCode(201);
        }else{
            judgeReport.setStatusCode(200);
        }

        judgeReport.setBasicCasesCorrectLine(BasicCasesCorrectCount);
        judgeReport.setBasicCasesPassed(basicCasesPassed);
        judgeReport.setSpecialCasesPassedList(specialCasesPassedList);
        judgeReport.setJudgeConfig(judgeConfig);
        setJudgeReport(judgeReport);
    }
}
