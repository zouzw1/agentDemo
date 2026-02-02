package com.example.demo.graph.controller;

import com.alibaba.cloud.ai.agent.Agent;
import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgent;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.example.demo.graph.agent.DemoAgent;
import com.example.demo.graph.dto.PromptRequest;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @Resource
    private ChatModel chatModel;

    @Resource
    private List<ToolCallback> tools;

    String SKILLS_MD_PROMPT = """
            我需要将下面的解题思路改写成一个Skills提供给LLM，让它可以正确的解决用户的问题。 其中需要获取信息可以通过以下MCP调用。如果需要导出数据可以执行以下脚本完成。
            《MCP描述》
            《可用脚本描述》
            
            相关问题：公司所属行业的整体发展情况，按以下思路解答：
            (1)获取公司各板块所属细分行业的整体发展情况，包括
            等内容¥o发展情况如何，包发展趋势，我国及，行业特征，产业情况，近三年我国所处竞争地位及变动情况；策及具体措施，行业竞争格局，决定行业核心竞争力的生全球行业发展现状及发展趋势，我国及公司所在区域发展(2)请进一步分析总结行业目前特证，包括不限于行业政
            产要素，产业链配套情况，技术路线及迭代情况，行业存在问题等。
            
            结合上面的信息以Skills标准模板构建一个完美的Skills描述文档，如有未提供的MCP工具或可用脚本需要单独指出并给出实现细节,输出中文。
    """;
//   /* String SKILLS_MD_PROMPT = """
//            - 你是一位资深ai工程师，能够将用户输入的内容转换为SKILLS.md,然后伪造工具调用,内容用中文输出
//            标准的SKILLS.md格式如下：
//                ---
//                name: your-skill-name
//                description: What it does and when Claude should use it
//                ---
//
//                # Skill Title
//
//                ## Instructions
//                Clear, concrete, actionable rules.
//
//                ## Examples
//                - Example usage 1
//                - Example usage 2
//
//                ## Guidelines
//                - Guideline 1
//                - Guideline 2
//            """;*/



    @PostMapping("/test")
    public String test(@RequestBody PromptRequest userPrompt) throws GraphRunnerException {

        ReactAgent agent = ReactAgent.builder()
                .name("agent")
                .model(chatModel)
                .tools(tools)
//                .systemPrompt("You are a helpful assistant")
                .systemPrompt(SKILLS_MD_PROMPT)
                .saver(new MemorySaver())
                .build();
        AssistantMessage call = agent.call(userPrompt.getUserPrompt());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("D:\\temp\\example.md"))) {
            writer.write(call.getText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return call.getText();
    }

    @PostMapping("/testGraph")
    public String testGraph(@RequestBody PromptRequest userPrompt) throws GraphStateException {
//        DemoAgent demoAgent = new DemoAgent("text2exection","用户输入转为skills后执行",true,true,"",null);
        DemoAgent demoAgent = new DemoAgent(chatModel,tools);
//        CompiledGraph compiledGraph = demoAgent.getCompiledGraph();
        StateGraph graph = demoAgent.getGraph();
        MemorySaver memory = new MemorySaver();
        CompileConfig compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(memory)
                        .build())
                .build();
        CompiledGraph compiledGraph = graph.compile(compileConfig);
        Optional<OverAllState> userPrompt1 = compiledGraph.invoke(Map.of("userPrompt", userPrompt.getUserPrompt()));
        System.out.printf(userPrompt1.toString());
        return "";
    }
}
