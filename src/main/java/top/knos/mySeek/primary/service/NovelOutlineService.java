package top.knos.mySeek.primary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import top.knos.mySeek.exception.NovelGenerationException;
import top.knos.mySeek.primary.vo.NovelOutline;

import java.util.UUID;

@Service
@Slf4j
public class NovelOutlineService {
    private final NovelStorageService storageService;
    private final NovelSupport novelSupport;

    public NovelOutlineService(NovelStorageService storageService, NovelSupport novelSupport) {
        this.storageService = storageService;
        this.novelSupport = novelSupport;
    }

    // 生成流程控制
    public NovelOutline generateNovel(String storySummary, Integer totalChapters) throws JsonProcessingException {
        //先看有没有历史记录
        NovelOutline outline = storageService.loadOutline();
        if(outline == null){
            log.debug("创建历史概要。");
            outline = generateOutline(storySummary, totalChapters);
            storageService.saveOutline(outline);
        }else {
            log.info("已读取小说历史概要。");
        }
        return outline;
    }

    private NovelOutline generateOutline(String summary, Integer totalChapters) {
        // 分步骤生成，确保质量
        log.info("开始生成总结");
        String detailedSummary = generateDetailedSummary(summary, totalChapters);
        log.info("生成总结完成:\n{}",detailedSummary);
        log.info("生成章节");
        String characters = generateCharacters(detailedSummary);
        log.info("生成章节:\n{}",detailedSummary);
        log.info("生成概要");
        String structuredOutline = generateStructuredOutline(detailedSummary, characters, totalChapters);
        log.info("生成概要:\n{}",structuredOutline);

        log.info("解析概要响应");
        return parseOutlineResponse(structuredOutline, totalChapters);
    }

    private String generateDetailedSummary(String summary, Integer totalChapters) {
        String prompt = """
                你是一个资深小说策划师，请根据简要概况生成详细的小说背景设定：
                
                原始概况：%s
                目标章节数：%d章
                
                请生成包含以下内容的详细设定：
                1. 故事背景世界观（时代背景、社会结构、地理环境）
                2. 核心矛盾冲突（主要矛盾、次要矛盾、潜在冲突）
                3. 故事主题深度（想要表达的思想、价值观、人生哲理）
                4. 情节发展脉络（起承转合的大致走向）
                5. 高潮设计思路（核心冲突爆发点、解决方案）
                
                要求：
                - 内容要足够支撑%d章的长篇小说
                - 设定要有深度和复杂性
                - 为后续人物设计提供充分背景
                - 所有内容使用简体中文
                
                请以段落形式详细阐述，不少于800字。
                """.formatted(summary, totalChapters, totalChapters);

        log.debug("概要提示词：\n{}",prompt);
        String conversationId = "detailed_summary_" + UUID.randomUUID();
        return novelSupport.chat(conversationId, prompt);
    }

    private String generateCharacters(String detailedSummary) {
        String prompt = """
                基于以下详细背景，设计小说的主要人物：
                
                背景设定：%s
                
                请设计以下角色：
                1. 主角：包括性格、背景、能力、目标、内心冲突
                2. 重要配角（3-4个）：各自的作用、关系、发展轨迹
                3. 反派角色：动机、手段、与主角的对立关系
                4. 关键支持角色：导师、朋友、家人等
                
                每个角色要求：
                - 性格要立体，有优缺点
                - 背景要符合世界观
                - 角色间要有复杂的关系网
                - 每个角色都要有成长弧线
                
                请详细描述每个角色，每个角色不少于200字。
                """.formatted(detailedSummary);

        log.debug("章节提示词：\n{}",prompt);
        String conversationId = "characters_" + UUID.randomUUID();
        return novelSupport.chat(conversationId, prompt);
    }

    private String generateStructuredOutline(String detailedSummary, String characters, Integer totalChapters) {
        // 计算篇章结构
        int partsCount = calculatePartsCount(totalChapters);
        int chaptersPerPart = totalChapters / partsCount;
        int remainingChapters = totalChapters % partsCount;

        String prompt = """
                你是一个专业小说大纲师，请根据以下信息生成完整的小说大纲：
                
                详细背景：%s
                
                角色设定：%s
                
                结构要求：
                1. 总章节数：严格限定为%d章
                2. 分为%d篇，每篇约%d章（允许±1章的误差）
                3. 按照经典三幕剧结构：
                   - 第一幕（建立期）：前25%%章节，世界观建立、角色登场
                   - 第二幕（发展期）：中50%%章节，冲突升级、角色成长
                   - 第三幕（高潮期）：后25%%章节，危机爆发、矛盾解决
                
                每章要求：
                1. 每章必须包含3-5个具体的情节点
                2. 每个情节点要包含：场景、冲突、角色反应、推动作用
                3. 每章至少埋下1-2个伏笔
                4. 章节间要有连贯性和递进关系
                
                输出格式要求：
                - 必须使用标准JSON格式
                - 所有引号使用英文双引号
                - 字符串内的引号要转义
                - 不要有多余的逗号
                
                JSON结构示例：
                {
                  "summary": "完整故事概要（不少于500字）",
                  "characterSettings": [
                    "主角：详细设定",
                    "配角1：详细设定",
                    "配角2：详细设定",
                    "反派：详细设定"
                  ],
                  "parts": [
                    {
                      "partTitle": "第一篇：起始篇",
                      "partSummary": "本篇概要（不少于200字）",
                      "chapters": [
                        {
                          "title": "第一章 具体标题",
                          "plotPoints": [
                            "情节点1：具体描述场景和冲突",
                            "情节点2：具体描述角色反应",
                            "情节点3：具体描述推动作用"
                          ],
                          "foreshadowings": [
                            "伏笔1：为后续情节做铺垫",
                            "伏笔2：埋下悬念"
                          ]
                        }
                      ]
                    }
                  ]
                }
                
                重要提醒：
                - 必须严格按照%d章生成
                - 每篇章节数要均匀分布
                - 确保JSON格式正确
                - 不要省略任何章节
                """.formatted(detailedSummary, characters, totalChapters, partsCount, chaptersPerPart, totalChapters);

        String conversationId = "structured_outline_" + UUID.randomUUID();
        return novelSupport.chat(conversationId, prompt);
    }

    private int calculatePartsCount(Integer totalChapters) {
        // 根据总章节数计算合适的篇数
        if (totalChapters <= 30) return 3;
        if (totalChapters <= 60) return 4;
        if (totalChapters <= 90) return 5;
        if (totalChapters <= 120) return 6;
        return 7;
    }

    private NovelOutline parseOutlineResponse(String response, Integer totalChapters) {
        try {
            // 分阶段处理混合内容
            String processedContent = novelSupport.preprocessResponse(response);
            log.debug("预处理后内容：\n{}", processedContent);

            String jsonContent = novelSupport.extractJsonFromMarkdown(processedContent);
            log.debug("提取的JSON内容：\n{}", jsonContent);

            NovelOutline outline = novelSupport.parseValidatedJson(jsonContent, totalChapters);

            // 验证章节数量
            int actualChapters = outline.parts().stream()
                    .mapToInt(part -> part.chapters().size())
                    .sum();

            if (actualChapters != totalChapters) {
                log.warn("章节数不匹配，预期{}章，实际{}章，尝试重新生成", totalChapters, actualChapters);
                // 可以选择重新生成或调整
            }

            return outline;
        } catch (Exception e) {
            log.error("原始响应内容：\n{}", response);
            log.error("解析大纲失败：\n{}", ExceptionUtils.getStackTrace(e));
            throw new NovelGenerationException("大纲解析失败: " + e.getMessage());
        }
    }
}