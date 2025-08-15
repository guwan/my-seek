package top.knos.mySeek.primary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import top.knos.mySeek.exception.NovelGenerationException;
import top.knos.mySeek.primary.vo.NovelChapter;
import top.knos.mySeek.primary.vo.NovelFeedback;
import top.knos.mySeek.primary.vo.NovelOutline;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class NovelChapterService {
    private final NovelStorageService storageService;
    private final NovelSupport novelSupport;

    public NovelChapterService(NovelStorageService storageService, NovelSupport novelSupport) {
        this.storageService = storageService;
        this.novelSupport = novelSupport;
    }

    public void generateChapter(NovelOutline outline, Integer totalChapters) {
        for (int i = 1; i <= totalChapters; i++) {
            log.info("开始生成第{}章小说", i);
            NovelChapter chapter = processChapter(i, outline);
            storageService.saveChapter(chapter);
        }
    }

    private NovelChapter processChapter(int chapterNumber, NovelOutline outline) {
        NovelChapter chapter;
        NovelFeedback feedback;
        int iteration = 0;
        int maxIterations = 5; // 增加迭代次数

        do {
            // 作者创作
            chapter = authorWriteChapter(chapterNumber, outline);
            log.info("第{}章作者创作完成，第{}遍。", chapterNumber, iteration + 1);

            // 字数检查
            if (chapter.content().length() < 2000) {
                log.warn("第{}章字数不足({})，重新生成", chapterNumber, chapter.content().length());
                chapter = expandChapter(chapter, outline);
            }

            // 编剧审核
            feedback = editorReview(chapter);
            log.info("第{}章编剧审核完成，第{}遍。", chapterNumber, iteration + 1);

            iteration++;
        } while (feedback.editorFeedback() != null && iteration < maxIterations);

        // 读者评审（减少评审次数，提高效率）
        feedback = collectReaderFeedback(chapter);
        for (int i = 0; i < 3; i++) {
            if (feedback.readerFeedbacks().isEmpty()) {
                break;
            }
            chapter = improveChapter(chapter, feedback);
            log.info("第{}章读者评审修改完成，第{}遍。", chapterNumber, i + 1);
        }

        return chapter;
    }

    private NovelChapter authorWriteChapter(int chapterNumber, NovelOutline outline) {
        String conversationId = "chapter_" + chapterNumber + "_" + UUID.randomUUID();

        // 获取当前章节的详细信息
        var currentChapterInfo = getCurrentChapterInfo(outline, chapterNumber);

        String prompt = """
                你是一位资深小说作家，请根据以下信息创作第%d章小说内容：
                
                【整体背景】
                %s
                
                【角色设定】
                %s
                
                【前文回顾】
                %s
                
                【本章大纲】
                标题：%s
                情节点：%s
                伏笔：%s
                
                【后续预告】
                %s
                
                【创作要求】
                1. 字数要求：严格控制在3000-4000字之间
                2. 内容要求：
                   - 必须包含至少3个戏剧冲突
                   - 每个情节点都要详细展开
                   - 对话要生动，推动剧情
                   - 环境描写要生动具体
                   - 心理描写要细腻深刻
                3. 结构要求：
                   - 开头要有吸引力
                   - 中间要有起伏
                   - 结尾要有悬念或转折
                4. 文学要求：
                   - 语言要优美流畅
                   - 情节要合理连贯
                   - 角色要立体鲜活
                   - 情感要真挚动人
                
                【重要提醒】
                - 必须严格按照大纲发展剧情
                - 不要脱离角色设定
                - 要为后续章节做好铺垫
                - 确保章节内容丰富完整
                
                请输出标准JSON格式：
                {
                  "title": "第%d章 具体标题",
                  "content": "完整章节内容（3000-4000字）",
                  "foreshadowings": ["新埋下的伏笔1", "新埋下的伏笔2"]
                }
                """.formatted(
                chapterNumber,
                outline.summary(),
                String.join("\\n", outline.characterSettings()),
                getPreviousSummary(outline, chapterNumber),
                currentChapterInfo.title(),
                String.join("；", currentChapterInfo.plotPoints()),
                String.join("；", currentChapterInfo.foreshadowings()),
                getNextForeshadowing(outline, chapterNumber),
                chapterNumber);

        return parseChapterResponse(novelSupport.chat(conversationId, prompt), chapterNumber);
    }

    private NovelChapter expandChapter(NovelChapter chapter, NovelOutline outline) {
        String conversationId = "expand_chapter_" + chapter.chapterNumber() + "_" + UUID.randomUUID();

        String prompt = """
                当前章节内容字数不足，请扩展以下章节内容：
                
                【当前内容】
                标题：%s
                内容：%s
                
                【扩展要求】
                1. 在原内容基础上扩展到3000-4000字
                2. 保持原有故事脉络不变
                3. 增加以下内容：
                   - 更详细的环境描写
                   - 更深入的心理描写
                   - 更丰富的对话内容
                   - 更细致的动作描写
                   - 更生动的细节刻画
                4. 确保扩展内容与原文风格一致
                5. 不要改变核心剧情
                
                请输出完整的扩展版本：
                {
                  "title": "%s",
                  "content": "扩展后的完整内容（3000-4000字）",
                  "foreshadowings": ["保持原有伏笔"]
                }
                """.formatted(chapter.title(), chapter.content(), chapter.title());

        return parseChapterResponse(novelSupport.chat(conversationId, prompt), chapter.chapterNumber());
    }

    private ChapterInfo getCurrentChapterInfo(NovelOutline outline, int chapterNumber) {
        int currentIndex = 0;
        for (var part : outline.parts()) {
            for (var chapter : part.chapters()) {
                currentIndex++;
                if (currentIndex == chapterNumber) {
                    return new ChapterInfo(chapter.title(), chapter.plotPoints(), chapter.foreshadowings());
                }
            }
        }
        return new ChapterInfo("章节标题", List.of("情节点"), List.of("伏笔"));
    }

    private record ChapterInfo(String title, List<String> plotPoints, List<String> foreshadowings) {}

    private String getPreviousSummary(NovelOutline outline, int currentChapter) {
        if (currentChapter <= 1) return "故事开始";

        StringBuilder summary = new StringBuilder();
        int chapterCount = 0;

        for (var part : outline.parts()) {
            for (var chapter : part.chapters()) {
                chapterCount++;
                if (chapterCount >= currentChapter) break;

                if (chapterCount > currentChapter - 4) { // 取前3章
                    summary.append("第").append(chapterCount).append("章：")
                            .append(String.join("；", chapter.plotPoints()))
                            .append("\\n");
                }
            }
            if (chapterCount >= currentChapter) break;
        }

        return summary.toString();
    }

    private String getNextForeshadowing(NovelOutline outline, int currentChapter) {
        if (currentChapter >= outline.parts().stream().mapToInt(p -> p.chapters().size()).sum()) {
            return "故事接近尾声，为结局做准备";
        }

        int chapterCount = 0;
        for (var part : outline.parts()) {
            for (var chapter : part.chapters()) {
                chapterCount++;
                if (chapterCount == currentChapter + 1) {
                    return "为下一章做铺垫：" + String.join("；", chapter.plotPoints());
                }
            }
        }

        return "为后续发展做铺垫";
    }

    private String getPreviousForeshadowing(NovelOutline outline, int currentChapter) {
        if (currentChapter <= 1) return "无";

        StringBuilder foreshadowings = new StringBuilder();
        int chapterCount = 0;

        for (var part : outline.parts()) {
            for (var chapter : part.chapters()) {
                chapterCount++;
                if (chapterCount >= currentChapter) break;

                if (!chapter.foreshadowings().isEmpty()) {
                    foreshadowings.append("第").append(chapterCount).append("章伏笔：")
                            .append(String.join("；", chapter.foreshadowings()))
                            .append("\\n");
                }
            }
            if (chapterCount >= currentChapter) break;
        }

        return foreshadowings.toString();
    }

    private NovelFeedback editorReview(NovelChapter chapter) {
        String conversationId = "chapter_review_" + chapter.chapterNumber() + "_" + UUID.randomUUID();
        String prompt = """
                请从专业编辑角度审核以下章节：
                
                【章节信息】
                标题：%s
                字数：%d字
                内容：%s
                
                【审核标准】
                1. 内容合规性：是否符合中国法律法规和社会主义核心价值观
                2. 逻辑合理性：情节是否合理，角色行为是否符合设定
                3. 字数要求：是否达到3000字以上
                4. 文学质量：语言是否流畅，描写是否生动
                5. 情节完整性：是否有明确的起承转合
                
                如果发现问题，请给出具体的修改建议；
                如果没有问题，请返回"审核通过"。
                """.formatted(chapter.title(), chapter.content().length(), chapter.content());

        String feedback = novelSupport.chat(conversationId, prompt);
        return new NovelFeedback(feedback.contains("审核通过") ? null : feedback, List.of());
    }

    private NovelFeedback collectReaderFeedback(NovelChapter chapter) {
        List<String> feedbacks = new ArrayList<>();

        // 减少评审数量，提高效率
        for (int i = 0; i < 5; i++) {
            String conversationId = "chapter_feedback_" + chapter.chapterNumber() + "_" + i + "_" + UUID.randomUUID();
            String prompt = """
                    请以普通读者身份对以下章节进行评价：
                    
                    【章节信息】
                    标题：%s
                    字数：%d字
                    内容：%s
                    
                    【评价要求】
                    1. 最吸引人的情节是什么？
                    2. 有什么需要改进的地方？
                    3. 给这一章打分（1-5分）
                    4. 是否想继续阅读下去？
                    
                    请简洁明了地给出评价。
                    """.formatted(chapter.title(), chapter.content().length(), chapter.content());

            String feedback = novelSupport.chat(conversationId, prompt);
            if (!feedback.contains("5分") && !feedback.contains("很好") && !feedback.contains("优秀")) {
                feedbacks.add(feedback);
            }
        }

        return new NovelFeedback(null, feedbacks);
    }

    private NovelChapter improveChapter(NovelChapter original, NovelFeedback feedback) {
        String conversationId = "chapter_improve_" + original.chapterNumber() + "_" + UUID.randomUUID();
        String prompt = """
                请根据读者反馈优化以下章节：
                
                【原始内容】
                标题：%s
                内容：%s
                
                【读者反馈】
                %s
                
                【优化要求】
                1. 保持原有故事脉络和风格
                2. 重点改进读者提到的问题
                3. 增强情节的吸引力
                4. 保持3000-4000字的篇幅
                5. 保留所有关键伏笔
                
                请输出优化后的版本：
                {
                  "title": "%s",
                  "content": "优化后的完整内容",
                  "foreshadowings": ["保持原有伏笔"]
                }
                """.formatted(original.title(), original.content(),
                String.join("\\n", feedback.readerFeedbacks()),
                original.title());

        return parseChapterResponse(novelSupport.chat(conversationId, prompt), original.chapterNumber());
    }

    private NovelChapter parseChapterResponse(String response, int chapterNumber) {
        try {
            ObjectMapper mapper = new JsonMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            String processedContent = novelSupport.preprocessResponse(response);
            log.debug("预处理后内容：\\n{}", processedContent);

            String jsonContent = novelSupport.extractJsonFromMarkdown(processedContent);
            log.debug("提取的JSON内容：\\n{}", jsonContent);

            JsonNode node = mapper.readTree(jsonContent);
            String content = node.path("content").asText();

            // 验证字数
            if (content.length() < 2000) {
                log.warn("章节内容字数不足：{} 字", content.length());
            }

            return new NovelChapter(
                    chapterNumber,
                    node.path("title").asText().replaceAll("[\\\\\\\\/:*?\"<>|]", "_"),
                    content
            );
        } catch (JsonProcessingException e) {
            log.error("章节解析失败:\\n原文为：{}\\n错误为:\\n{}", response, ExceptionUtils.getStackTrace(e));
            throw new NovelGenerationException("章节解析失败: " + e.getMessage());
        }
    }
}