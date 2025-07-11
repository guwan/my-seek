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
    public void generateChapter(NovelOutline outline,Integer totalChapters) {
        for (int i = 1; i <= totalChapters; i++) {
            log.info("开始生成第{}章小说",i);
            NovelChapter chapter = processChapter(i, outline);
            storageService.saveChapter(chapter);
        }
    }

    private NovelChapter processChapter(int chapterNumber, NovelOutline outline) {
        NovelChapter chapter;
        NovelFeedback feedback;
        int iteration = 0;

        do {
            // 作者创作
            chapter = authorWriteChapter(chapterNumber, outline);
            log.info("第{}章作者创作完成，第{}遍。",chapterNumber,iteration+1);

            // 编剧审核
            feedback = editorReview(chapter);
            log.info("第{}章编剧审核完成，第{}遍。",chapterNumber,iteration+1);

            // 如果需要修改则循环
        } while (feedback.editorFeedback() != null && iteration++ < 3);

        // 读者评审
        feedback = collectReaderFeedback(chapter);
        for(int i =0;i<5;i++){
            if (feedback.readerFeedbacks().isEmpty()){
                break;
            }
            chapter = improveChapter(chapter, feedback);
            log.info("第{}章编读者评审修改完成，第{}遍。",chapterNumber,i+1);
        }

        return chapter;
    }

    private NovelChapter authorWriteChapter(int chapterNumber, NovelOutline outline) {
        String conversationId = "chapter_" + chapterNumber + "_" + UUID.randomUUID();
        String prompt = """
            请根据完整大纲创作第%d章：
            整体概要：%s
            前章提要：%s
            本章重点：%s
            后续伏笔：%s
            
            要求：
            1. 小说章节需要有2000-3000个中文，3个以上戏剧冲突
            2. 合理使用前文伏笔（特别是[%s]）
            3. 为后续章节埋设新伏笔
            4. 保持角色行为一致性
            
            输出JSON：{
                "title": "章节标题",
                "content": "章节内容",
                "foreshadowings": ["新伏笔1", "新伏笔2"]
            }""".formatted(
                chapterNumber,
                outline.summary(),
                getPreviousSummary(outline, chapterNumber),
                outline.plotPoints().get(chapterNumber-1),
                getNextForeshadowing(outline, chapterNumber),
                getPreviousForeshadowing(outline, chapterNumber));

        return parseChapterResponse(novelSupport.chat(conversationId, prompt));
    }

    private String getPreviousSummary(NovelOutline outline, int currentChapter) {
        if(currentChapter <= 1) return "无前文";
        int start = Math.max(0, currentChapter-4); // 取前3章内容
        return String.join(" -> ", outline.plotPoints().subList(start, currentChapter-1));
    }

    private String getNextForeshadowing(NovelOutline outline, int currentChapter) {
        if(currentChapter >= 120) return "结局铺垫";
        return "为第" + (currentChapter+1) + "章[" +
                outline.plotPoints().get(currentChapter).split("伏笔")[0] + "]做铺垫";
    }

    private String getPreviousForeshadowing(NovelOutline outline, int currentChapter) {
        if(currentChapter <= 1) return "无";
        return String.join(",", outline.plotPoints().subList(0, currentChapter-1)
                .stream().filter(p -> p.contains("伏笔")).toList());
    }

    private NovelFeedback editorReview(NovelChapter chapter) {
        String conversationId = "chapter_review_" + chapter.chapterNumber() + "_" + UUID.randomUUID();
        String prompt = """
                请从以下角度审核小说章节：
                标题：%s
                内容：%s
                
                审核要求：
                1. 是否符合中国法律法规
                2. 是否包含敏感内容
                3. 是否存在逻辑漏洞
                4. 是否符合社会主义核心价值观
                5. 是否大于2000千字
                
                发现问题请返回修改建议，无问题返回空""".formatted(chapter.title(), chapter.content());

        String feedback = novelSupport.chat(conversationId,prompt);
        return new NovelFeedback(feedback.isEmpty() ? null : feedback, List.of());
    }

    private NovelFeedback collectReaderFeedback(NovelChapter chapter) {
        List<String> feedbacks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String conversationId = "chapter_feedback_" + chapter.chapterNumber() + "_"+i+ "_" + UUID.randomUUID();
            String prompt = """
                    请以读者身份对以下章节进行点评：
                    标题：%s
                    内容：%s
                    
                    点评要求：
                    1. 指出最吸引人的部分
                    2. 提出改进建议
                    3. 评分（1-5星）
                    4. 是否推荐继续阅读""".formatted(chapter.title(), chapter.content());

            feedbacks.add(novelSupport.chat(conversationId,prompt));
        }
        return new NovelFeedback(null, feedbacks);
    }

    private NovelChapter improveChapter(NovelChapter original, NovelFeedback feedback) {
        String conversationId = "chapter_improve_" + original.chapterNumber() + "_" + UUID.randomUUID();
        String prompt = """
                请根据以下反馈修改小说章节：
                原内容：%s
                读者反馈：%s
                
                修改要求：
                1. 保持原有风格不变
                2. 重点改进人物刻画
                3. 增强情节合理性
                4. 保留所有关键伏笔""".formatted(original.content(), String.join("\n", feedback.readerFeedbacks()));

        return parseChapterResponse(novelSupport.chat(conversationId,prompt));
    }

    // 章节解析方法
    private NovelChapter parseChapterResponse(String response) {
        try {
            ObjectMapper mapper = new JsonMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


            // 分阶段处理混合内容
            String processedContent = novelSupport.preprocessResponse(response);
            log.debug("预处理后内容：\n{}", processedContent);

            String jsonContent = novelSupport.extractJsonFromMarkdown(processedContent);
            log.debug("提取的JSON内容：\n{}", jsonContent);

            JsonNode node = mapper.readTree(jsonContent);
            return new NovelChapter(
                    -1, // 章节号由外部传入
                    node.path("title").asText().replaceAll("[\\\\/:*?\"<>|]", "_"),
                    node.path("content").asText()
            );
        } catch (JsonProcessingException e) {
            log.error("章节解析失败:\n原文为：{}\n错误为:\n{}",response, ExceptionUtils.getStackTrace(e));
            throw new NovelGenerationException("章节解析失败: " + e.getMessage());
        }
    }
}
