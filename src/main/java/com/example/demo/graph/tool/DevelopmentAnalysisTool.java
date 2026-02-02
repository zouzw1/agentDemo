package com.example.demo.graph.tool;

import org.springframework.ai.chat.model.ToolContext;

import java.util.function.BiFunction;

public class DevelopmentAnalysisTool implements BiFunction<String, ToolContext, String> {

    @Override
    public String apply(String s, ToolContext toolContext) {
        return "分析完成";
    }
}