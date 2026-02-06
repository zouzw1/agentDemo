package com.example.demo.graph.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;

import java.util.function.BiFunction;

@Slf4j
public class IdentifyIndustryChallengesTool implements BiFunction<String, ToolContext, String> {

    @Override
    public String apply(String s, ToolContext toolContext) {
        log.info("use: IdentifyIndustryChallengesTool");
        log.info(toolContext.getContext().toString());
        return "当前核心挑战：国际技术管制加剧“卡脖子”风险，高端人才稀缺，研发投入巨大且失败率高。供应链高度全球化，地缘政治与库存波动易导致中断。ESG压力剧增，芯片制造的高能耗与化学品使用面临严格的环保与碳减排要求。";
    }
}