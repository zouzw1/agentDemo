package com.example.demo.graph.service;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SkillExecutionService {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private List<ToolCallback> tools;

    /**
     * 解析SKILL.md文件，提取执行步骤
     */
    public List<String> parseSkillSteps(String skillMdPath) throws IOException {
        List<String> steps = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(skillMdPath))) {
            String line;
            boolean inInstructions = false;
            while ((line = reader.readLine()) != null) {
                // 检查是否进入Instructions部分
                if (line.contains("## Instructions")) {
                    inInstructions = true;
                    continue;
                }
                
                // 如果遇到下一个主要章节标题，则结束
                if (inInstructions && line.startsWith("## ") && !line.contains("Instructions")) {
                    break;
                }
                
                // 提取工具调用步骤
                if (inInstructions) {
                    // 匹配"调用工具xxx"的模式
                    Pattern pattern = Pattern.compile("调用工具(\\w+)");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String toolName = matcher.group(1);
                        steps.add(toolName);
                    }
                }
            }
        }
        return steps;
    }

    /**
     * 按顺序执行技能步骤
     */
    public String executeSkillSteps(String userPrompt) throws Exception {
        // 解析技能步骤
        String skillMdPath = "src/main/resources/skills/industry-analyst/SKILL.md";
        List<String> steps = parseSkillSteps(skillMdPath);
        
        if (steps.isEmpty()) {
            throw new IllegalStateException("未找到有效的技能执行步骤");
        }

        StringBuilder executionLog = new StringBuilder();
        executionLog.append("# 技能执行日志\n\n");
        executionLog.append("执行技能: industry-analyst\n");
        executionLog.append("用户请求: ").append(userPrompt).append("\n\n");
        executionLog.append("## 执行步骤\n\n");

        // 按顺序执行每个步骤
        for (int i = 0; i < steps.size(); i++) {
            String toolName = steps.get(i);
            executionLog.append("### 步骤 ").append(i + 1).append(": 调用工具 ").append(toolName).append("\n");
            
            try {
                String result = invokeTool(toolName, userPrompt);
                executionLog.append("**执行结果:**\n").append(result).append("\n\n");
                log.info("步骤 {} 执行成功: 调用工具 {}", i + 1, toolName);
            } catch (Exception e) {
                executionLog.append("**执行失败:** ").append(e.getMessage()).append("\n\n");
                log.error("步骤 {} 执行失败: 调用工具 {}, 错误: {}", i + 1, toolName, e.getMessage());
            }
        }

        // 生成最终报告
        executionLog.append("## 最终报告\n\n");
        executionLog.append(generateFinalReport(userPrompt));

        return executionLog.toString();
    }

    /**
     * 调用具体工具
     */
    private String invokeTool(String toolName, String input) throws Exception {
        switch (toolName) {
            case "get_developmentAnalysis":
                return invokeDevelopmentAnalysis(input);
            case "get_CharacteristicsAnalysis":
                return invokeCharacteristicsAnalysis(input);
            case "get_EvaluatePolicyEnvironmentTool":
                return invokeEvaluatePolicyEnvironment(input);
            case "get_DetermineCoreCompetitivenessFactorsTool":
                return invokeDetermineCoreCompetitivenessFactors(input);
            case "get_IdentifyIndustryChallengesTool":
                return invokeIdentifyIndustryChallenges(input);
            default:
                throw new IllegalArgumentException("未知工具: " + toolName);
        }
    }

    /**
     * 调用行业发展分析工具
     */
    private String invokeDevelopmentAnalysis(String input) throws Exception {
        // 这里应该调用实际的工具，暂时返回模拟数据
        ReactAgent agent = ReactAgent.builder()
                .name("development-analysis-agent")
                .model(chatModel)
                .tools(tools)
                .systemPrompt("你是一个行业发展分析专家，请分析输入行业的整体发展情况。")
                .saver(new MemorySaver())
                .build();
        
        AssistantMessage response = agent.call("请分析" + input + "行业的发展现状与趋势");
        return response.getText();
    }

    /**
     * 调用行业特征分析工具
     */
    private String invokeCharacteristicsAnalysis(String input) throws Exception {
        ReactAgent agent = ReactAgent.builder()
                .name("characteristics-analysis-agent")
                .model(chatModel)
                .tools(tools)
                .systemPrompt("你是一个行业特征分析专家，请分析输入行业的核心特征。")
                .saver(new MemorySaver())
                .build();
        
        AssistantMessage response = agent.call("请分析" + input + "行业的核心特征");
        return response.getText();
    }

    /**
     * 调用政策环境评估工具
     */
    private String invokeEvaluatePolicyEnvironment(String input) throws Exception {
        ReactAgent agent = ReactAgent.builder()
                .name("policy-environment-agent")
                .model(chatModel)
                .tools(tools)
                .systemPrompt("你是一个政策环境分析专家，请评估输入行业的政策环境。")
                .saver(new MemorySaver())
                .build();
        
        AssistantMessage response = agent.call("请评估" + input + "行业的政策环境");
        return response.getText();
    }

    /**
     * 调用核心竞争力因素确定工具
     */
    private String invokeDetermineCoreCompetitivenessFactors(String input) throws Exception {
        ReactAgent agent = ReactAgent.builder()
                .name("core-competitiveness-agent")
                .model(chatModel)
                .tools(tools)
                .systemPrompt("你是一个竞争力分析专家，请确定输入行业的核心竞争要素。")
                .saver(new MemorySaver())
                .build();
        
        AssistantMessage response = agent.call("请确定" + input + "行业的核心竞争要素");
        return response.getText();
    }

    /**
     * 调用行业挑战识别工具
     */
    private String invokeIdentifyIndustryChallenges(String input) throws Exception {
        ReactAgent agent = ReactAgent.builder()
                .name("industry-challenges-agent")
                .model(chatModel)
                .tools(tools)
                .systemPrompt("你是一个风险管理专家，请识别输入行业面临的主要挑战。")
                .saver(new MemorySaver())
                .build();
        
        AssistantMessage response = agent.call("请识别" + input + "行业面临的主要挑战");
        return response.getText();
    }

    /**
     * 生成最终综合报告
     */
    private String generateFinalReport(String userPrompt) throws Exception {
        ReactAgent agent = ReactAgent.builder()
                .name("final-report-agent")
                .model(chatModel)
                .tools(tools)
                .systemPrompt("你是一个行业分析报告撰写专家，请根据前面的分析结果生成一份完整的行业分析报告。")
                .saver(new MemorySaver())
                .build();
        
        String prompt = "基于前面的各项分析，请为" + userPrompt + "生成一份完整的行业分析报告，包含：发展现状与趋势、行业核心特征、政策环境分析、关键竞争要素、挑战与风险、综合结论等部分。";
        
        AssistantMessage response = agent.call(prompt);
        return response.getText();
    }
}