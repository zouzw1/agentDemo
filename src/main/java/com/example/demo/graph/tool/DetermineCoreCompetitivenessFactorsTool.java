package com.example.demo.graph.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;

import java.util.function.BiFunction;

@Slf4j
public class DetermineCoreCompetitivenessFactorsTool implements BiFunction<String, ToolContext, String> {

    @Override
    public String apply(String s, ToolContext toolContext) {
        log.info("use: DetermineCoreCompetitivenessFactorsTool");
        return "确定核心竞争力要素如下：关键成功因素为持续技术迭代与可靠供应链。所需关键资源是顶尖人才、专利组合与资本。差异化机会在于特定应用（如AI、汽车）芯片的敏捷定制设计。可持续优势依赖于快速将创新商业化的生态合作与客户绑定能力。" ;
    }
}