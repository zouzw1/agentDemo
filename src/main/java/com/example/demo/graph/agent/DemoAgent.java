package com.example.demo.graph.agent;


import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.example.demo.graph.node.Content2MardDownNode;
import com.example.demo.graph.node.ExecutionNode;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

/**
 * 测试提交
 */
public class DemoAgent extends BaseAgent {

    private ChatModel chatModel;

    private List<ToolCallback> tools;

    public DemoAgent(String name, String description, boolean includeContents, boolean returnReasoningContents, String outputKey, KeyStrategy outputKeyStrategy) {
        super(name, description, includeContents, returnReasoningContents, outputKey, outputKeyStrategy);
    }

    public DemoAgent(ChatModel chatModel, List<ToolCallback> tools) {
        super("text2exection","用户输入转为skills后执行",true,true,"",null);
        this.chatModel = chatModel;
        this.tools = tools;
    }

    @Override
    public Node asNode(boolean includeContents, boolean returnReasoningContents) {
        return null;
    }

    @Override
    protected StateGraph initGraph() throws GraphStateException {
        StateGraph stateGraph = new StateGraph(new KeyStrategyFactory() {
            @Override
            public Map<String, KeyStrategy> apply() {
                return Map.of("c2m", KeyStrategy.REPLACE,"exection",KeyStrategy.REPLACE);
            }
        })
                .addNode("c2m", AsyncNodeAction.node_async(Content2MardDownNode.builder().chatModel(chatModel).tools(tools).build()))
                .addNode("exection", AsyncNodeAction.node_async( new ExecutionNode()));

        stateGraph.addEdge(StateGraph.START, "c2m");
        stateGraph.addEdge("c2m", "exection");
        stateGraph.addEdge("exection", StateGraph.END);

        return stateGraph;
    }


    /*public CompiledGraph getCompiledGraph() throws GraphStateException {
        var memory = new MemorySaver();
        var compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(memory)
                        .build())
                .interruptBefore("human_review")  // 在人工审核前中断
                .build();

        return this.graph.compile(compileConfig);
    }*/

}
