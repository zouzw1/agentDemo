package com.example.demo.graph.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ScriptContextCheckerTool implements BiFunction<String, ToolContext, String> {

    private static final String SCRIPTS_BASE_PATH = "skills/industry-analyst/scripts/";
    private static final String SCRIPTS_PATTERN = "classpath*:skills/industry-analyst/scripts/*.js";
    
    // 存储已加载的脚本信息
    private final Map<String, ScriptInfo> loadedScripts = new HashMap<>();
    
    @Override
    public String apply(String input, ToolContext toolContext) {
        log.info("执行脚本上下文检查工具调用: {}", input);
        log.info(toolContext.getContext().toString());
        try {
            // 解析输入参数
            CheckRequest request = parseInput(input);
            
            if (request == null) {
                return "错误：无法解析脚本检查请求";
            }
            
            // 根据请求类型执行相应操作
            String result;
            switch (request.action) {
                case CHECK_LOADED:
                    result = checkLoadedScripts();
                    break;
                case CHECK_AVAILABLE:
                    result = checkAvailableScripts();
                    break;
                case CHECK_SPECIFIC:
                    result = checkSpecificScript(request.scriptName);
                    break;
                case REFRESH_CONTEXT:
                    result = refreshScriptContext();
                    break;
                default:
                    result = "未知检查类型";
            }
            
            return formatResult(request.action, request.scriptName, result);
            
        } catch (Exception e) {
            log.error("脚本上下文检查失败", e);
            return "脚本上下文检查失败: " + e.getMessage();
        }
    }

    /**
     * 解析输入字符串，提取检查类型和脚本名称
     */
    private CheckRequest parseInput(String input) {
        // 支持多种输入格式：
        // 1. "检查已加载的脚本"
        // 2. "查看内存中的脚本"
        // 3. "检查脚本 developmentAnalysis.js 是否已加载"
        // 4. "列出可用的脚本"
        // 5. "刷新脚本上下文"
        // 6. "check loaded scripts"
        // 7. "is script developmentAnalysis.js loaded"
        
        // 检查已加载脚本的模式
        if (input.matches(".*(检查|查看|check).*已加载.*脚本.*") || 
            input.matches(".*(内存|context).*脚本.*") ||
            input.matches(".*(loaded|已加载).*scripts?.*")) {
            return new CheckRequest(Action.CHECK_LOADED, null);
        }
        
        // 检查可用脚本的模式
        if (input.matches(".*(列出|查看|list).*可用.*脚本.*") || 
            input.matches(".*(available|可用).*scripts?.*")) {
            return new CheckRequest(Action.CHECK_AVAILABLE, null);
        }
        
        // 刷新上下文的模式
        if (input.matches(".*(刷新|更新|refresh).*上下文.*") || 
            input.matches(".*(refresh|update).*context.*")) {
            return new CheckRequest(Action.REFRESH_CONTEXT, null);
        }
        
        // 检查特定脚本的模式
        Pattern[] checkPatterns = {
            Pattern.compile("(?:检查|查看|check)\\s*(?:脚本)?\\s*([\\w.-]+(?:\\.js)?)\\s*(?:是否)?\\s*(?:已加载|loaded)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:is|是否)\\s*([\\w.-]+(?:\\.js)?)\\s*(?:loaded|已加载)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("script\\s+([\\w.-]+(?:\\.js)?)\\s+(?:status|状态)", Pattern.CASE_INSENSITIVE)
        };
        
        for (Pattern pattern : checkPatterns) {
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                String scriptName = matcher.group(1);
                // 确保脚本名以.js结尾
                if (!scriptName.endsWith(".js")) {
                    scriptName += ".js";
                }
                return new CheckRequest(Action.CHECK_SPECIFIC, scriptName);
            }
        }
        
        return null;
    }

    /**
     * 检查已加载的脚本
     */
    private String checkLoadedScripts() {
        StringBuilder result = new StringBuilder();
        result.append("## 已加载的脚本上下文\n\n");
        
        if (loadedScripts.isEmpty()) {
            result.append("当前没有任何脚本被加载到内存中。\n\n");
            result.append("💡 **提示**: 使用 '刷新脚本上下文' 命令来扫描并加载可用的脚本。\n");
            return result.toString();
        }
        
        result.append("当前内存中已加载的脚本数量: ").append(loadedScripts.size()).append("\n\n");
        result.append("### 已加载脚本详情:\n\n");
        
        int index = 1;
        for (Map.Entry<String, ScriptInfo> entry : loadedScripts.entrySet()) {
            String scriptName = entry.getKey();
            ScriptInfo info = entry.getValue();
            
            result.append("**").append(index++).append(". ").append(scriptName).append("**\n");
            result.append("- 加载时间: ").append(info.loadTime).append("\n");
            result.append("- 文件大小: ").append(info.fileSize).append(" 字节\n");
            result.append("- 函数数量: ").append(info.functionCount).append("\n");
            result.append("- 主要函数: ").append(String.join(", ", info.mainFunctions)).append("\n");
            result.append("- 状态: ").append(info.status).append("\n\n");
        }
        
        return result.toString();
    }

    /**
     * 检查可用的脚本文件
     */
    private String checkAvailableScripts() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(SCRIPTS_PATTERN);
        
        StringBuilder result = new StringBuilder();
        result.append("## 可用脚本文件\n\n");
        result.append("在 ").append(SCRIPTS_BASE_PATH).append(" 目录下找到以下脚本文件:\n\n");
        
        if (resources.length == 0) {
            result.append("❌ 未找到任何JavaScript脚本文件。\n");
            return result.toString();
        }
        
        result.append("总共找到 ").append(resources.length).append(" 个脚本文件:\n\n");
        
        for (int i = 0; i < resources.length; i++) {
            Resource resource = resources[i];
            String filename = resource.getFilename();
            if (filename != null) {
                boolean isLoaded = loadedScripts.containsKey(filename);
                String status = isLoaded ? "✅ 已加载" : "⭕ 未加载";
                
                result.append(i + 1).append(". ").append(filename).append(" ").append(status).append("\n");
                
                // 显示文件基本信息
                try {
                    long fileSize = resource.contentLength();
                    result.append("   - 大小: ").append(fileSize).append(" 字节\n");
                } catch (Exception e) {
                    result.append("   - 大小: 无法获取\n");
                }
            }
        }
        
        return result.toString();
    }

    /**
     * 检查特定脚本的状态
     */
    private String checkSpecificScript(String scriptName) throws Exception {
        StringBuilder result = new StringBuilder();
        result.append("## 脚本 '").append(scriptName).append("' 状态检查\n\n");
        
        // 检查是否已加载
        if (loadedScripts.containsKey(scriptName)) {
            ScriptInfo info = loadedScripts.get(scriptName);
            result.append("✅ 该脚本已在内存中加载\n\n");
            result.append("**加载详情:**\n");
            result.append("- 加载时间: ").append(info.loadTime).append("\n");
            result.append("- 文件大小: ").append(info.fileSize).append(" 字节\n");
            result.append("- 函数数量: ").append(info.functionCount).append("\n");
            result.append("- 主要函数: ").append(String.join(", ", info.mainFunctions)).append("\n");
            result.append("- 状态: ").append(info.status).append("\n");
        } else {
            result.append("⭕ 该脚本未在内存中加载\n\n");
            
            // 检查文件是否存在
            String scriptPath = SCRIPTS_BASE_PATH + scriptName;
            ClassPathResource scriptResource = new ClassPathResource(scriptPath);
            
            if (scriptResource.exists()) {
                result.append("🔍 文件存在，但未加载到内存中\n");
                result.append("- 文件路径: ").append(scriptPath).append("\n");
                try {
                    result.append("- 文件大小: ").append(scriptResource.contentLength()).append(" 字节\n");
                } catch (Exception e) {
                    result.append("- 文件大小: 无法获取\n");
                }
                result.append("\n💡 **建议**: 使用 '刷新脚本上下文' 命令来加载此脚本\n");
            } else {
                result.append("❌ 文件不存在\n");
                result.append("- 搜索路径: ").append(scriptPath).append("\n");
                result.append("\n🔍 **提示**: 请检查文件名是否正确\n");
            }
        }
        
        return result.toString();
    }

    /**
     * 刷新脚本上下文（扫描并加载所有可用脚本）
     */
    private String refreshScriptContext() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(SCRIPTS_PATTERN);
        
        StringBuilder result = new StringBuilder();
        result.append("## 刷新脚本上下文\n\n");
        result.append("正在扫描 ").append(SCRIPTS_BASE_PATH).append(" 目录...\n\n");
        
        if (resources.length == 0) {
            result.append("❌ 未找到任何JavaScript脚本文件。\n");
            return result.toString();
        }
        
        result.append("发现 ").append(resources.length).append(" 个脚本文件，开始加载分析...\n\n");
        
        int loadedCount = 0;
        int errorCount = 0;
        
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename != null) {
                try {
                    ScriptInfo info = analyzeScript(resource);
                    loadedScripts.put(filename, info);
                    loadedCount++;
                    result.append("✅ ").append(filename).append(" - 加载成功\n");
                } catch (Exception e) {
                    errorCount++;
                    result.append("❌ ").append(filename).append(" - 加载失败: ").append(e.getMessage()).append("\n");
                    log.warn("脚本分析失败: {}", filename, e);
                }
            }
        }
        
        result.append("\n### 刷新结果总结:\n");
        result.append("- 成功加载: ").append(loadedCount).append(" 个脚本\n");
        result.append("- 加载失败: ").append(errorCount).append(" 个脚本\n");
        result.append("- 当前内存中脚本总数: ").append(loadedScripts.size()).append(" 个\n");
        
        return result.toString();
    }

    /**
     * 分析脚本文件，提取基本信息
     */
    private ScriptInfo analyzeScript(Resource resource) throws Exception {
        ScriptInfo info = new ScriptInfo();
        info.loadTime = new Date();
        info.status = "已加载";
        
        try {
            info.fileSize = resource.contentLength();
        } catch (Exception e) {
            info.fileSize = -1;
        }
        
        // 分析脚本内容
        List<String> functions = new ArrayList<>();
        int lineCount = 0;
        
        try (InputStream is = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                
                // 查找函数定义
                if (line.contains("function ") || line.contains("=>") || line.matches(".*\\w+\\s*[:=]\\s*function.*")) {
                    // 简单提取函数名
                    Pattern funcPattern = Pattern.compile("(?:function\\s+(\\w+)|([\\w$]+)\\s*[:=]\\s*(?:function|\\([^)]*\\)\\s*=>))");
                    Matcher matcher = funcPattern.matcher(line);
                    if (matcher.find()) {
                        String funcName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                        if (funcName != null && !funcName.trim().isEmpty()) {
                            functions.add(funcName.trim());
                        }
                    }
                }
            }
        }
        
        info.functionCount = functions.size();
        // 只保留前5个主要函数
        info.mainFunctions = functions.stream().limit(5).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        return info;
    }

    /**
     * 格式化执行结果
     */
    private String formatResult(Action action, String scriptName, String result) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("## 脚本上下文检查结果\n\n");
        
        switch (action) {
            case CHECK_LOADED:
                formatted.append("**检查类型**: 已加载脚本状态\n\n");
                break;
            case CHECK_AVAILABLE:
                formatted.append("**检查类型**: 可用脚本文件\n\n");
                break;
            case CHECK_SPECIFIC:
                formatted.append("**检查类型**: 特定脚本状态\n");
                formatted.append("**脚本名称**: ").append(scriptName).append("\n\n");
                break;
            case REFRESH_CONTEXT:
                formatted.append("**操作类型**: 刷新脚本上下文\n\n");
                break;
        }
        
        if (result != null && !result.trim().isEmpty()) {
            formatted.append(result);
        } else {
            formatted.append("**执行结果**: 检查完成，无内容返回\n");
        }
        
        return formatted.toString();
    }

    /**
     * 操作类型枚举
     */
    private enum Action {
        CHECK_LOADED,     // 检查已加载的脚本
        CHECK_AVAILABLE,  // 检查可用的脚本文件
        CHECK_SPECIFIC,   // 检查特定脚本
        REFRESH_CONTEXT   // 刷新脚本上下文
    }

    /**
     * 检查请求数据类
     */
    private static class CheckRequest {
        final Action action;
        final String scriptName;
        
        CheckRequest(Action action, String scriptName) {
            this.action = action;
            this.scriptName = scriptName;
        }
    }

    /**
     * 脚本信息数据类
     */
    private static class ScriptInfo {
        Date loadTime;
        long fileSize;
        int functionCount;
        List<String> mainFunctions;
        String status;
    }
}