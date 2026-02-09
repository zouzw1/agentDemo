package com.example.demo.graph.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;

import java.util.function.BiFunction;

@Slf4j
public class EvaluatePolicyEnvironmentTool implements BiFunction<String, ToolContext, String> {

    @Override
    public String apply(String s, ToolContext toolContext) {
        log.info("use： EvaluatePolicyEnvironmentTool");
        return "当前半导体行业政策以“自主可控”为核心目标，采用“超常规措施”全链条推动核心技术攻关，通过税收优惠、资金补贴、产业基金引导与鼓励兼并重组等组合拳，旨在加速国产替代，健全产业生态，实现产业链的整体进阶与安全。";
    }
}