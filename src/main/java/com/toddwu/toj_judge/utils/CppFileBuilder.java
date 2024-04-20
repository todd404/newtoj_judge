package com.toddwu.toj_judge.utils;

import com.toddwu.toj_judge.pojo.ProblemConfig;
import com.toddwu.toj_judge.pojo.TypeMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Component
public class CppFileBuilder {
    @Autowired
    TypeMap typeMap;

    public void buildCppFile(String code, ProblemConfig problemConfig){
        StringBuilder fileContent = new StringBuilder();
        String basePath = "E:\\document\\new_toj\\toj_judge\\cpp_template\\";

        //头文件
        String headerFileContent = readFileToString(basePath + "headers.h") + "\n";

        fileContent.append(headerFileContent);
        fileContent.append("using namespace std;\n");

        //参数处理函数
        StringBuilder argFunctionContent = new StringBuilder();
        List<String> functionNames = new ArrayList<>();

        for(String arg : problemConfig.getArgumentTypeList()){
            Map<String, String> argumentFunctionMap = typeMap.getArgumentFunctionMap();

            String functionName = argumentFunctionMap.get(arg);
            functionNames.add(functionName);

            String function = readFileToString(basePath + functionName + ".cpp") + "\n";
            argFunctionContent.append(function);
        }
        argFunctionContent.append("\n");

        fileContent.append(argFunctionContent);

        //返回值处理函数
        String returnType = typeMap.getCppTypeMap().get(problemConfig.getReturnType());
        String returnFunctionName = typeMap.getReturnFunctionMap().get(problemConfig.getReturnType());
        String returnFunctionContent = readFileToString(basePath + "%s.cpp".formatted(returnFunctionName)) + "\n";

        fileContent.append(returnFunctionContent);

        //TODO:改成代码占位符
        fileContent.append("\n<code></code>\n");

        //cpp main函数
        StringBuilder cppSolutionFunction = new StringBuilder();
        cppSolutionFunction.append("solution.%s(".formatted(problemConfig.getFunctionName()));
        for(int i  = 0; i < functionNames.size(); i++){
            cppSolutionFunction.append("%s(input_args[%d]),".formatted(functionNames.get(i), i));
        }
        cppSolutionFunction.deleteCharAt(cppSolutionFunction.length() - 1);
        cppSolutionFunction.append(")");

        String mainTemplate = readFileToString(basePath + "main.cpp");
        String mainContent = mainTemplate.formatted(problemConfig.getArgumentTypeList().size(),
                returnType,
                cppSolutionFunction,
                "%s(result)".formatted(returnFunctionName));

        fileContent.append(mainContent);
    }

    private String readFileToString(String path){
        String result = "";

        try {
            result = Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

}
