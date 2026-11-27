package top.knos.mySeek.agent.service;

import top.knos.mySeek.agent.entity.AgentDefinition;
import top.knos.mySeek.agent.entity.ConnectorDefinition;
import top.knos.mySeek.agent.entity.SkillDefinition;

import java.util.List;

/** 系统内置专家 / 技能 / 连接器定义，首次启动写入，之后不覆盖用户改动。 */
public final class BuiltinCatalog {

    /** 内置项归属 id，0 表示系统 */
    public static final long SYSTEM_USER = 0L;

    private BuiltinCatalog() {}

    public record BuiltinAgent(String code, String name, String description, String icon,
                               String systemPrompt, String skillsJson, String modelCode) {}

    public record BuiltinSkill(String code, String name, String description, String icon,
                               String promptTemplate, String toolsJson) {}

    public record BuiltinConnector(String code, String name, String description, String icon,
                                   ConnectorDefinition.Type type, String toolsJson) {}

    public static List<BuiltinAgent> agents() {
        return List.of(
                new BuiltinAgent("general", "通用助理", "擅长拆解目标、按步骤完成通用任务",
                        "bot", "你是一名严谨的通用任务助理。基于给定目标与约束，把工作拆成清晰步骤，优先使用可用工具获取事实，输出可直接使用的结果。",
                        "[]", null),
                new BuiltinAgent("knowledge_expert", "知识库专家", "检索与整理知识库资料，给出有依据的回答",
                        "book-open", "你是一名知识库专家。回答前先用知识库检索工具获取资料，严格基于检索结果作答，区分事实与推断；资料不足时明确说明。",
                        "[\"knowledge_retrieval\",\"web_research\"]", null),
                new BuiltinAgent("writing_expert", "写作专家", "协助构思、撰写与润色作品草稿",
                        "pen-line", "你是一名写作专家。先理解题材、风格与约束，再输出结构清晰、语言流畅的草稿；保存草稿属于写操作，需用户批准后再执行。",
                        "[\"draft_writing\"]", null),
                new BuiltinAgent("research_expert", "研究分析师", "联网搜索并归纳资料，输出结构化结论",
                        "telescope", "你是一名研究分析师。使用联网搜索收集多方资料，归纳共同结论与分歧点，输出带信息源的结构化摘要，不编造事实。",
                        "[\"web_research\",\"knowledge_retrieval\"]", null)
        );
    }

    public static List<BuiltinSkill> skills() {
        return List.of(
                new BuiltinSkill("knowledge_retrieval", "知识库检索", "从用户知识库中检索相关内容",
                        "search", "先用 knowledge_list 确认可用文档，再用 knowledge_search 检索相关内容，topK 控制在 5 以内。",
                        "[\"knowledge_list\",\"knowledge_search\"]"),
                new BuiltinSkill("web_research", "联网调研", "搜索公开资料并归纳要点",
                        "globe", "用 web_search 搜索关键词，对结果去噪并归纳要点；必须标明信息来源，不确定的结论要显式说明。",
                        "[\"web_search\"]"),
                new BuiltinSkill("draft_writing", "草稿写作", "撰写并保存作品草稿",
                        "file-pen", "先规划章节结构，再分段撰写；需要保存时使用 novel_write_draft（写操作，需用户批准）。",
                        "[\"novel_read\",\"novel_write_draft\"]"),
                new BuiltinSkill("evidence_summary", "证据归纳", "基于多来源资料输出结构化结论",
                        "list-checks", "将各来源结果按一致、冲突、缺失三类归纳，输出结论与依据，避免长篇复述原文。",
                        "[]")
        );
    }

    public static List<BuiltinConnector> connectors() {
        return List.of(
                new BuiltinConnector("builtin_tools", "MySeek 内置工具", "知识库、写作与联网搜索工具集",
                        "wrench", ConnectorDefinition.Type.BUILTIN,
                        "[\"knowledge_search\",\"knowledge_list\",\"novel_read\",\"novel_write_draft\",\"web_search\"]"),
                new BuiltinConnector("mcp", "MCP 外部工具", "通过 MCP 服务器接入的外部工具（需先在配置中启用）",
                        "plug-zap", ConnectorDefinition.Type.MCP, "[]")
        );
    }

    public static AgentDefinition toEntity(BuiltinAgent a) {
        AgentDefinition e = new AgentDefinition();
        e.setCode(a.code());
        e.setName(a.name());
        e.setDescription(a.description());
        e.setIcon(a.icon());
        e.setSystemPrompt(a.systemPrompt());
        e.setSkillsJson(a.skillsJson());
        e.setModelCode(a.modelCode());
        return e;
    }

    public static SkillDefinition toEntity(BuiltinSkill s) {
        SkillDefinition e = new SkillDefinition();
        e.setCode(s.code());
        e.setName(s.name());
        e.setDescription(s.description());
        e.setIcon(s.icon());
        e.setPromptTemplate(s.promptTemplate());
        e.setToolsJson(s.toolsJson());
        return e;
    }

    public static ConnectorDefinition toEntity(BuiltinConnector c) {
        ConnectorDefinition e = new ConnectorDefinition();
        e.setCode(c.code());
        e.setName(c.name());
        e.setDescription(c.description());
        e.setIcon(c.icon());
        e.setType(c.type());
        e.setToolsJson(c.toolsJson());
        return e;
    }
}
