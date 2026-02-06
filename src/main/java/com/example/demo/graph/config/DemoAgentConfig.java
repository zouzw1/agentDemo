package com.example.demo.graph.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.SpringAiSkillAdvisor;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.example.demo.graph.tool.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DemoAgentConfig {

    @Bean
    public ChatModel chatModel(){
        return DashScopeChatModel.builder()
                .dashScopeApi(DashScopeApi.builder()
                        .apiKey("sk-e149a15431ec48ceb6a839cab679762e")
                        .build())
                .build();
    }

    @Bean
    public List<ToolCallback> tools(){
        List<ToolCallback> toolCallbackList = new ArrayList<>();
        toolCallbackList.add(FunctionToolCallback.builder("get_weather", new WeatherTool())
                .description("Get weather for a given city")
                .inputType(String.class)
                .build()
        );
        toolCallbackList.add(FunctionToolCallback.builder("get_CharacteristicsAnalysis", new CharacteristicsAnalysisTool())
                .description("Get analysis for a given industry，Include normal administrative measures of the industry and specific measures, as well as the competitive landscape of the industry")
                .inputType(String.class)
                .build()
        );
        toolCallbackList.add(FunctionToolCallback.builder("get_developmentAnalysis", new DevelopmentAnalysisTool())
                .description("Get analysis for a given industry，include Analyze Market Trends & Development Status")
                .inputType(String.class)
                .build()
        );
        toolCallbackList.add(FunctionToolCallback.builder("get_EvaluatePolicyEnvironmentTool", new EvaluatePolicyEnvironmentTool())
                .description("Get analysis for a given industry，Include policy evaluate")
                .inputType(String.class)
                .build()
        );
        toolCallbackList.add(FunctionToolCallback.builder("get_DetermineCoreCompetitivenessFactorsTool", new DetermineCoreCompetitivenessFactorsTool())
                .description("Get analysis for a given industry，Include Determine Core Competitiveness Factors")
                .inputType(String.class)
                .build()
        );
        toolCallbackList.add(FunctionToolCallback.builder("get_IdentifyIndustryChallengesTool", new IdentifyIndustryChallengesTool())
                .description("Get analysis for a given industry，Include industry challenges")
                .inputType(String.class)
                .build()
        );
        return toolCallbackList;
    }

    @Bean
    public List<SkillMetadata> getSkills() throws IOException {
        ClasspathSkillRegistry classpathSkillRegistry = ClasspathSkillRegistry.builder()
                .build();
        SpringAiSkillAdvisor springAiSkillAdvisor = SpringAiSkillAdvisor.builder()
                .skillRegistry(classpathSkillRegistry)
                .build();
        return springAiSkillAdvisor.listSkills();
    }

    @Bean
    public SkillsAgentHook getSkillsAgentHook() {
        return SkillsAgentHook.builder()
                .skillRegistry(ClasspathSkillRegistry.builder().build())
                .build();
    }

    @Bean
    public ReactAgent getReactAgentBean(ChatModel chatModel, SkillsAgentHook skillsAgentHook){

        final String SKILL_STEP_PROMPT = """
                    You are a helpful assistant with access to skills.
            
                    【核心指令】
                    1.在回复的任何部分，当你需要应用特定SKILLS来完成分析或生成内容时，必须在相应位置**明确声明**你所调用的具体SKILL名称。技能声明格式为：`-> 调用技能：[技能名称]`。
            """;
        List<ToolCallback> toolList = new ArrayList<>();
        toolList.add(FunctionToolCallback.builder("execute_script", new ScriptExecutorTool())
                .description("当需要使用JavaScript脚本时，调用此工具")
                .inputType(String.class)
                .build());

        ReactAgent agent = ReactAgent.builder()
                .name("agent")
                .model(chatModel)
                .systemPrompt(SKILL_STEP_PROMPT)
                .hooks(skillsAgentHook)
//                    .tools(tools)
                .tools(toolList)
                .saver(new MemorySaver())
                .build();
        return agent;
    }

}
