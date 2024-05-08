package com.toddwu.toj_judge.judge;

import cn.hutool.core.io.FileUtil;
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
public class JavaJudge extends Judge{
    File outputFile;

    @SneakyThrows
    @Override
    public void judgeCode() {
        outputFile = new File(judgeConfig.getUuid() + "/out.jar");

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
        } finally {
            judgeReportMQSender.send(getJudgeConfig().getUuid());
            FileUtil.del(uuidDir);
        }
    }

    void complete() throws IOException, InterruptedException, CompleteException {
        JudgeReport judgeReport = pullJudgeReport();
        judgeReport.setStatusCode(101);
        putJudgeReport(judgeReport);

        ProcessBuilder processBuilder = new ProcessBuilder("javac",
                codeFile.getAbsolutePath());
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

        File executableFile = new File(judgeConfig.getUuid() + "/Main.class");

        Boolean setExecutableResult = executableFile.setExecutable(true);
        ProcessBuilder processBuilder = new ProcessBuilder(
                "/usr/bin/time",
                "-f", "%e,%M",
                "java", "Main");
        processBuilder.directory(uuidDir);

        run(processBuilder);
    }
}
