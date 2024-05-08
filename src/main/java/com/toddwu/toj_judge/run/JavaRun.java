package com.toddwu.toj_judge.run;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.toddwu.toj_judge.exception.CompleteException;
import com.toddwu.toj_judge.exception.RunningException;
import com.toddwu.toj_judge.pojo.run.RunReport;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

@Component
@Scope("prototype")
public class JavaRun extends Run{
    File outputFile;

    @Override
    public void runForResult() throws JsonProcessingException {
        try {
            initFiles();
            complete();
            run();
        } catch (IOException | InterruptedException e) {
            reportError(501, "内部错误");
            throw new RuntimeException(e);
        } catch (CompleteException e) {
            reportError(400, e.getMessage());
        } catch (RunningException e) {
            reportError(500, e.getMessage());
        }finally {
            FileUtil.del(uuidDir);
        }
    }

    void complete() throws IOException, InterruptedException, CompleteException {
        RunReport runReport = pullRunReport();
        runReport.setStatusCode(101);
        runReport.setMsg("编译中");
        putRunReport(runReport);

        ProcessBuilder processBuilder = new ProcessBuilder("javac",
                codeFile.getAbsolutePath());
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
        ProcessBuilder processBuilder = new ProcessBuilder("java", "Main");
        processBuilder.directory(uuidDir);

        run(processBuilder);
    }
}
