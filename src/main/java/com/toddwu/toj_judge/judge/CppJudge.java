package com.toddwu.toj_judge.judge;

import com.toddwu.toj_judge.exception.CompleteException;
import com.toddwu.toj_judge.exception.RunningException;
import com.toddwu.toj_judge.pojo.judge.JudgeReport;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

@Component
@Scope("prototype")
public class CppJudge extends Judge{
    File outputFile;

    @SneakyThrows
    @Override
    @Async
    public void judgeCode() {
        outputFile = new File(judgeConfig.getUuid() + "/out.out");

        try {
            initFiles();
            complete();
            run();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RunningException e) {
            reportError(500, e.getMessage());
        } catch (CompleteException e){
            reportError(400, e.getMessage());
        }
    }

    void complete() throws IOException, InterruptedException, CompleteException {
        JudgeReport judgeReport = pullJudgeReport();
        judgeReport.setStatusCode(101);
        putJudgeReport(judgeReport);

        ProcessBuilder processBuilder = new ProcessBuilder("clang++",
                codeFile.getAbsolutePath(),
                "-o", uuidDir.getPath() + "/" + "out.out");
        Process process = processBuilder.start();

        Integer returnCode = process.waitFor();
        if(returnCode != 0){
            BufferedReader errorReader = process.errorReader();
            String result = "";
            StringBuilder errMsg = new StringBuilder();
            while ((result = errorReader.readLine()) != null){
                errMsg.append(result);
            }
            throw new CompleteException(errMsg.toString());
        }
    }

    void run() throws Exception, RunningException {
        JudgeReport judgeReport = pullJudgeReport();
        judgeReport.setStatusCode(102);
        putJudgeReport(judgeReport);

        File executableFile = new File(judgeConfig.getUuid() + "/out.out");

        Boolean setExecutableResult = executableFile.setExecutable(true);
        ProcessBuilder processBuilder = new ProcessBuilder(
                "/usr/bin/time",
                "-f", "%e,%M",
                executableFile
                        .getAbsolutePath());
        run(processBuilder);
    }
}
