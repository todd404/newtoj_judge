package com.toddwu.toj_judge.judge;

import com.toddwu.toj_judge.exception.RunningException;
import com.toddwu.toj_judge.pojo.JudgeReport;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
        } catch (Exception e){
            reportError(404, e.getMessage());
        }
    }

    void complete() throws IOException, InterruptedException {
        JudgeReport judgeReport = getJudgeReport();
        judgeReport.setStatusCode(101);
        setJudgeReport(judgeReport);

        ProcessBuilder processBuilder = new ProcessBuilder("clang++",
                codeFile.getAbsolutePath(),
                "-o", uuidDir.getPath() + "/" + "out.out");
        Process process = processBuilder.start();

        process.waitFor();
    }

    void run() throws Exception, RunningException {
        JudgeReport judgeReport = getJudgeReport();
        judgeReport.setStatusCode(102);
        setJudgeReport(judgeReport);

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
