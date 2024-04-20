package com.toddwu.toj_judge.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.toddwu.toj_judge.exception.CompleteException;
import com.toddwu.toj_judge.exception.RunningException;
import com.toddwu.toj_judge.pojo.run.RunReport;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

@Component
public class CppRun extends Run{
    File outputFile;

    @Override
    @Async
    public void runForResult() throws JsonProcessingException {
        outputFile = new File(runConfig.getUuid() + "/out.out");
        RunReport runReport = pullRunReport();

        try {
            initFiles();
            complete();
            run();
        } catch (IOException | InterruptedException e) {
            runReport = pullRunReport();
            runReport.setStatusCode(501);
            runReport.setMsg("内部错误");
            putRunReport(runReport);
            throw new RuntimeException(e);
        } catch (CompleteException e) {
            runReport = pullRunReport();
            runReport.setStatusCode(400);
            runReport.setMsg(e.getMessage());
            putRunReport(runReport);
        } catch (RunningException e) {
            runReport = pullRunReport();
            runReport.setStatusCode(500);
            runReport.setMsg(e.getMessage());
            putRunReport(runReport);
        }
    }

    void complete() throws IOException, InterruptedException, CompleteException {
        RunReport runReport = pullRunReport();
        runReport.setStatusCode(101);
        runReport.setMsg("编译中");
        putRunReport(runReport);

        ProcessBuilder processBuilder = new ProcessBuilder("clang++",
                codeFile.getAbsolutePath(),
                "-o", uuidDir.getPath() + "/" + "out.out");
        Process process = processBuilder.start();

        Integer returnCode = process.waitFor();
        if(returnCode != 0){
            BufferedReader errorReader = process.errorReader();
            String line = "";
            StringBuilder errMsg  = new StringBuilder();
            while((line = errorReader.readLine()) != null){
                errMsg.append(line);
            }
            throw new CompleteException(errMsg.toString());
        }
    }

    void run() throws IOException, RunningException, InterruptedException {
        RunReport runReport = pullRunReport();
        runReport.setStatusCode(102);
        runReport.setMsg("运行中");
        putRunReport(runReport);

        File executableFile = new File(runConfig.getUuid() + "/out.out");
        Boolean setExecutableResult = executableFile.setExecutable(true);
        ProcessBuilder processBuilder = new ProcessBuilder(executableFile.getAbsolutePath());
        run(processBuilder);
    }
}
