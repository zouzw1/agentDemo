package com.example.demo.graph.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;

import java.util.function.BiFunction;

@Slf4j
public class CharacteristicsAnalysisTool implements BiFunction<String, ToolContext, String> {

    @Override
    public String apply(String s, ToolContext toolContext) {
        log.info("use: CharacteristicsAnalysisTool");
        return "半导体行业具有技术、资本、人才密集，产业链全球化分工，周期性波动显著的特征。当前，各国均出台强力产业政策（如美国《芯片法案》）推动本土制造与供应链安全。我国在封测等环节具备优势，但在先进制程制造、核心设备与材料等领域仍面临追赶挑战。";
    }
}