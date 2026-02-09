package com.example.demo.graph.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ScriptExecutorTool implements BiFunction<String, ToolContext, String> {

    private static final String SCRIPTS_BASE_PATH = "skills/industry-analyst/scripts/";
    
    @Override
    public String apply(String input, ToolContext toolContext) {
        log.info("执行脚本工具调用: {}", input);
        try {
            // 解析输入参数，提取脚本名称和参数
            ScriptExecutionRequest request = parseInput(input);
            
            if (request == null) {
                return "错误：无法解析脚本执行请求";
            }
            
            // 执行脚本
            String result = executeNodeScript(request);
            log.info(result);
            return formatResult(request.scriptName, result);
            
        } catch (Exception e) {
            log.error("脚本执行失败", e);
            return "脚本执行失败: " + e.getMessage();
        }
    }

    /**
     * 解析输入字符串，提取脚本名称和参数
     */
    private ScriptExecutionRequest parseInput(String input) {
        // 支持多种输入格式：
        // 1. "执行脚本 developmentAnalysis1.js"
        // 2. "运行 developmentAnalysis1.js 脚本"
        // 3. "调用脚本 developmentAnalysis1.js"
        // 4. "execute developmentAnalysis1.js"
        
        Pattern[] patterns = {
            Pattern.compile("(?:执行|运行|调用|execute)\\s*(?:脚本)?\\s*([\\w.-]+(?:\\.js)?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("script\\s+([\\w.-]+(?:\\.js)?)", Pattern.CASE_INSENSITIVE)
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                String scriptName = matcher.group(1);
                // 确保脚本名以.js结尾
                if (!scriptName.endsWith(".js")) {
                    scriptName += ".js";
                }
                return new ScriptExecutionRequest(scriptName, "");
            }
        }
        
        return null;
    }

    /**
     * 执行Node.js脚本
     */
    private String executeNodeScript(ScriptExecutionRequest request) throws Exception {
        // 构建脚本完整路径
        String scriptPath = SCRIPTS_BASE_PATH + request.scriptName;
        
        // 检查脚本文件是否存在
        ClassPathResource scriptResource = new ClassPathResource(scriptPath);
        if (!scriptResource.exists()) {
            throw new FileNotFoundException("脚本文件不存在: " + scriptPath);
        }
        
        // 创建临时文件来执行脚本
        Path tempScript = Files.createTempFile("script_", ".js");
        try {
            // 复制脚本内容到临时文件
            try (InputStream is = scriptResource.getInputStream();
                 OutputStream os = Files.newOutputStream(tempScript)) {
                is.transferTo(os);
            }
            
            // 构建Node.js执行命令
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("node", tempScript.toString());
            
            // 设置工作目录为脚本所在目录，以便相对路径引用
            processBuilder.directory(tempScript.getParent().toFile());
            
            // 启动进程
            Process process = processBuilder.start();
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            // 读取标准输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // 读取错误输出
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }
            
            // 等待进程完成，设置超时时间
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("脚本执行超时");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("脚本执行失败，退出码: " + exitCode + "\n错误信息: " + errorOutput.toString());
            }
            
            return output.toString();
            
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(tempScript);
            } catch (IOException e) {
                log.warn("无法删除临时脚本文件: {}", tempScript, e);
            }
        }
    }

    /**
     * 格式化执行结果
     */
    private String formatResult(String scriptName, String result) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("## 脚本执行结果\n\n");
        formatted.append("**脚本名称**: ").append(scriptName).append("\n\n");
        
        if (result != null && !result.trim().isEmpty()) {
            formatted.append("**执行输出**:\n");
            formatted.append("```\n");
            formatted.append(result);
            formatted.append("\n```\n");
        } else {
            formatted.append("**执行输出**: 脚本执行完成，无输出内容\n");
        }
        
        return formatted.toString();
    }

    /**
     * 脚本执行请求数据类
     */
    private static class ScriptExecutionRequest {
        final String scriptName;
        final String parameters;
        
        ScriptExecutionRequest(String scriptName, String parameters) {
            this.scriptName = scriptName;
            this.parameters = parameters;
        }
    }
}