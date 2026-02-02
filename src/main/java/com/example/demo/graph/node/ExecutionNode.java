package com.example.demo.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class ExecutionNode implements NodeAction {

    private final String EXECTION_PROMPT = """
            - role: skills使用者
            - describe: 根据md内容执行具体的工具调用,若无对应工具则输出:"未找到对应工具"
            - 将每一布的输入输出,进行输出
            - 返回格式为json
            """;

    @Resource
    private ChatModel chatModel;

    @Resource
    private List<ToolCallback> tools;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Optional<Object> toolResult = state.value("tool_result");
        ReactAgent agent = ReactAgent.builder()
                .name("agent")
                .model(chatModel)
                .tools(tools)
//                .systemPrompt("You are a helpful assistant")
                .systemPrompt(EXECTION_PROMPT)
                .saver(new MemorySaver())
                .build();
        AssistantMessage call = agent.call(toolResult.get().toString());
        String result = call.getText();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("D:\\temp\\example.md"))) {
            writer.write(call.getText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("LLM执行md完成: {}", call.getText());
        return Map.of(
                "execution_resukt", result
        );
    }
}
