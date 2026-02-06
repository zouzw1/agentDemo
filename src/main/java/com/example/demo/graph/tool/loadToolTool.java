package com.example.demo.graph.tool;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;

import java.util.function.BiFunction;

@Slf4j
public class loadToolTool implements BiFunction<String, ToolContext, String> {

    @Resource
    private ReactAgent reactAgentBean;


    @Override
    public String apply(String city, ToolContext toolContext) {


        return null;

    }
}