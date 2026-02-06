package com.example.demo.graph.controller;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.graph.agent.DemoAgent;
import com.example.demo.graph.dto.PromptRequest;
import com.example.demo.graph.service.SkillExecutionService;
import com.example.demo.graph.tool.ScriptContextCheckerTool;
import com.example.demo.graph.tool.ScriptExecutorTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/demo")
@Slf4j
public class DemoController {

    @Resource
    private ChatModel chatModel;

    @Resource
    private List<ToolCallback> tools;

    @Resource
    private List<SkillMetadata> skills;

    @Resource
    private SkillsAgentHook skillsAgentHook;

    @Resource
    private ReactAgent reactAgentBean;

    // 1. agentScope skill渐进式披露相关源码
    // 2. skill 调用时，能否不走script,走工具调用工具

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


    String SKILLS_PROMPT = """
                -你是一名技能分析师，需要完成以下工作：
                1.列出所有技能
                2.使用列出技能中第一个技能
                3.将技能中的每一步调用出入参都进行保存
                4.中文形式输出     
            """;

    @PostMapping("/test")
    public String test(@RequestBody PromptRequest userPrompt) throws GraphRunnerException {

        ReactAgent agent = ReactAgent.builder()
                .name("agent")
                .model(chatModel)
                .tools(tools)
                .systemPrompt("You are a helpful assistant")
                .hooks(skillsAgentHook)
//                .systemPrompt(SKILLS_MD_PROMPT)
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
        DemoAgent demoAgent = new DemoAgent(chatModel, tools);
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


    /**
     * 测试技能执行
     * 调用案例：
     * POST http://localhost:8080/demo/testSkills
     * Content-Type: application/json
     * <p>
     * {
     * "userPrompt": "根据技能列表的执行技能，按照技能的描述按步骤执行，如果缺少参数请自行模拟。并将每个技能Instructions中每一步执行出入参保存，最后返回执行流程和结果，使用中文回复"
     * }
     *
     * @param userPrompt
     * @return
     * @throws GraphStateException
     * @throws GraphRunnerException
     * @throws IOException
     */
    @PostMapping("/testSkills")
    public String testSkills(@RequestBody PromptRequest userPrompt) throws GraphStateException, GraphRunnerException, IOException {

        ReactAgent agent = ReactAgent.builder()
                .name("agent")
                .model(chatModel)
//                .systemPrompt("You are a helpful assistant")
                .systemPrompt("You are a helpful assistant, you have some skills , skills list: " + skills)
                .saver(new MemorySaver())
                .build();

        AssistantMessage call = agent.call(userPrompt.getUserPrompt());
        return call.getText();
    }


    private final String SKILL_STEP_PROMPT = """
                    You are a helpful assistant with access to skills.
            
                    【核心指令】
                    1.在回复的任何部分，当你需要应用特定SKILLS来完成分析或生成内容时，必须在相应位置**明确声明**你所调用的具体SKILL名称。技能声明格式为：`-> 调用技能：[技能名称]`。
            """;

    @PostMapping("/skillsStepDemo")
    public String skillsStepDemo(@RequestBody PromptRequest userPrompt) throws Exception {

        /*List<ToolCallback> toolList = new ArrayList<>();
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
                .build();*/
        AssistantMessage call = reactAgentBean.call(userPrompt.getUserPrompt());
        log.info(call.getText());
        return call.getText();
    }

    private final String NO_SKILL_STEP_PROMPT = """
                    请你作为专业的分析助手，完成行业分析。
            
                    【核心指令】
                    1.根据现有的工具进行调用,具体步骤如下：
                            1. 调用工具get_developmentAnalysis获取行业发展相关分析
            
                            2. 调用工具get_CharacteristicsAnalysis，分析总结行业目前特征
            
                            3. 调用工具get_EvaluatePolicyEnvironmentTool，评估政策环境
            
                            4. 调用工具get_DetermineCoreCompetitivenessFactorsTool，确定核心竞争性因素
            
                            5. 调用工具get_IdentifyIndustryChallengesTool，识别行业挑战
            
            """;

    @PostMapping("/noSkillsStepDemo")
    public String noSkillsStepDemo(@RequestBody PromptRequest userPrompt) throws GraphStateException, GraphRunnerException, IOException {

        ReactAgent agent = ReactAgent.builder()
                .name("agent")
                .model(chatModel)
                .systemPrompt(NO_SKILL_STEP_PROMPT)
//                .hooks(skillsAgentHook)
                .tools(tools)
                .saver(new MemorySaver())
                .build();
        AssistantMessage call = agent.call(userPrompt.getUserPrompt());
        System.out.println(JSONObject.toJSON(call.getText()).toString());
        return call.getText();
    }

}
