package com.example.demo.graph.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

import java.util.function.BiFunction;

@Slf4j
public class DevelopmentAnalysisTool implements BiFunction<String, ToolContext, String> {

    @Override
    public String apply(String s, ToolContext toolContext) {
        log.info("use： DevelopmentAnalysisTool");
        log.info(toolContext.getContext().toString());
        return "作为初创半导体公司，初期专注于细分设计领域，享受政策与资本红利，但面临技术迭代、人才争夺及供应链波动风险，需快速构建技术壁垒并实现客户导入以生存发展。";
    }
}