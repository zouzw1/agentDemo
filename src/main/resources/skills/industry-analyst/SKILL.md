---
name: industry-analyst
description: Analyze industry development, competitive positioning, and strategic insights using analytical frameworks. Use when analyzing company sectors, conducting industry research, evaluating market trends, or preparing strategic reports.
allowed-tools: Read, Grep, Glob
--- 

# Industry Analyst

A comprehensive industry analysis skill that examines company sector development, competitive positioning, and strategic insights based on specific analytical frameworks.

## Quick start

"Analyze the electric vehicle industry in China, focusing on market trends and competitive landscape."

## Instructions

严格按照顺序执行下列步骤，可以按需查询resources中的文档:

### Part 1: Industry Development Overview

1. 调用工具execute_script，执行脚本获取行业发展现状与趋势
   - 输入: "执行脚本 developmentAnalysis.js"
   - 或者: "运行 developmentAnalysis.js 脚本"

### Part 2: Industry Feature Analysis

2. 评估政策环境
   - 输入: "执行脚本 evaluatePolicyEnvironment.js"
   - 或者: "运行 evaluatePolicyEnvironment.js 脚本"

3. 识别行业挑战
   - 输入: "执行脚本 identifyIndustryChallenges.js"
   - 或者: "运行 identifyIndustryChallenges.js 脚本"

**输出格式：**
  Markdown文档，包含以下章节：
1. 发展现状与趋势
2. 行业核心特征
3. 政策环境分析
4. 关键竞争要素
5. 挑战与风险
6. 综合结论
7. 校验结果
8. 如果涉及到script，则将各script执行结果返回到markdown文档中，并给出script执行结果
 - 具体实例如下： scirpt名称：执行结果
9. 如果读取了resource，则将资源内容返回到markdown文档中，并给出资源内容

## Examples

### Example 1: Basic industry analysis request
