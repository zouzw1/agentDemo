package com.example.demo.graph.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.observation.SpringAiAlibabaKind;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.SkillPromptConstants;
import com.alibaba.cloud.ai.graph.skills.SpringAiSkillAdvisor;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.example.demo.graph.tool.DevelopmentAnalysisTool;
import com.example.demo.graph.tool.WeatherTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
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
        toolCallbackList.add(FunctionToolCallback.builder("get_developmentAnalysis", new DevelopmentAnalysisTool())
                .description("Get analysis for a given industry")
                .inputType(String.class)
                .build()
        );
        return toolCallbackList;
    }
    @Bean
    public List<SkillMetadata> getSkills() throws IOException {
        ClasspathSkillRegistry classpathSkillRegistry = ClasspathSkillRegistry.builder()
                .build();
        SpringAiSkillAdvisor.Builder builder = SpringAiSkillAdvisor.builder();
        builder.skillRegistry(classpathSkillRegistry);
        SpringAiSkillAdvisor springAiSkillAdvisor = builder.build();
        int skillCount = springAiSkillAdvisor.getSkillCount();
        System.out.printf(skillCount + "");
        List<SkillMetadata> skillMetadata = springAiSkillAdvisor.listSkills();
        System.out.printf(skillMetadata.toString());
        return skillMetadata;
    }
}
