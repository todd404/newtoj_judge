package com.toddwu.toj_judge.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toddwu.toj_judge.exception.RunningException;
import com.toddwu.toj_judge.pojo.judge.JudgeReport;
import com.toddwu.toj_judge.pojo.run.RunConfig;
import com.toddwu.toj_judge.pojo.run.RunReport;
import com.toddwu.toj_judge.utils.MyUtils;
import com.toddwu.toj_judge.utils.RedisCache;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Component
@Getter
@Setter
public abstract class Run {
    @Autowired
    RedisCache redisCache;

    RunConfig runConfig;

    File templateFile, testFile, answerFile, codeFile , uuidDir;

    void initFiles() throws IOException {
        templateFile = new File(runConfig.getUuid() + "/template." + runConfig.getLanguage());
        testFile = new File(runConfig.getUuid() + "/test.txt");
        answerFile = new File("/mnt/d/toj_files/run/" + runConfig.getUuid() + "/answer.txt");
        codeFile = new File(runConfig.getUuid() + "/Main." + runConfig.getLanguage());

        createUuidDir();
        copyFiles();
        buildRealCodeFile();
    }

    Boolean createUuidDir(){
        uuidDir = new File(runConfig.getUuid());
        return uuidDir.mkdir();
    }

    public abstract void runForResult() throws JsonProcessingException;

    void copyFiles() throws IOException {
        String basePath = "/mnt/d/toj_files/run/" + runConfig.getUuid();
        String runTestCaseFile = basePath + "/test.txt";
        String templateFilePath = basePath + "/template." + runConfig.getLanguage();

        Files.copy(Path.of(runTestCaseFile), testFile.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Path.of(templateFilePath), templateFile.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    void buildRealCodeFile() throws IOException {
        String templateCode = MyUtils.readFileToString(templateFile.getPath());
        String realCode = templateCode.replace("<code></code>", runConfig.getCode());

        Boolean err = codeFile.createNewFile();

        FileWriter fileWriter = new FileWriter(codeFile);
        fileWriter.write(realCode);
        fileWriter.close();
    }

    RunReport pullRunReport() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String msg = redisCache.getCacheObject("run:" + runConfig.getUuid());

        return objectMapper.readValue(msg, RunReport.class);
    }

    void putRunReport(RunReport runReport) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        redisCache.setCacheObject("run:" + runConfig.getUuid(), objectMapper.writeValueAsString(runReport), 10, TimeUnit.MINUTES);
    }

    void reportError(Integer code, String msg) throws JsonProcessingException {
        RunReport judgeReport = pullRunReport();
        judgeReport.setStatusCode(code);
        judgeReport.setMsg(msg);
        putRunReport(judgeReport);
    }

    void run(ProcessBuilder processBuilder) throws RunningException, IOException, InterruptedException {
        processBuilder.redirectInput(testFile);
        answerFile.createNewFile();
        processBuilder.redirectOutput(answerFile);

        Process process = processBuilder.start();

        var waitResult = process.waitFor(1, TimeUnit.MINUTES);
        if(!waitResult){
            //超时
            throw new RunningException("运行超时！");
        }

        String errorLine ;
        ArrayList<String> errorLineList = new ArrayList<>();
        while ((errorLine = process.errorReader().readLine()) != null){
            errorLineList.add(errorLine);
        }

        if(errorLineList.size() > 1){
            StringBuilder result = new StringBuilder();
            for(int i = 0; i < errorLineList.size(); i++){
                result.append(errorLineList.get(i)).append("\n");
            }
            throw new RunningException(result.toString());
        }

        Integer exitValue = process.exitValue();
        if(exitValue != 0){
            throw new RunningException("运行出错！");
        }

        RunReport runReport = pullRunReport();
        runReport.setStatusCode(200);
        putRunReport(runReport);
    }
}
