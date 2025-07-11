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
            outline = generateOutline(storySummary,totalChapters);
            storageService.saveOutline(outline);
        }else {
            log.info("已读取小说历史概要。");
        }
        return outline;
    }

    private NovelOutline generateOutline(String summary, Integer totalChapters) {
        String prompt = """
                你是一个资深小说创作AI，请根据以下故事概况生成完整的小说大纲：
                            概况：%s
                            要求：
                            1. 所有内容必须使用简体中文
                            2. 包含主要角色设定（至少3个主要角色）
                            3. 总章节数严格限定为%d章
                            4. 将总章节数分为若干篇（parts），
                            5. 每篇包含章节数应均匀分布（误差不超过±5章）
                            6. 故事结构需包含三幕剧结构（开端占25%%、发展占50%%、高潮占25%%）
                            7. 每篇需要有关键冲突点和起承转合
                            8. 每个章节必须包含3-5个关键情节点
                            9. 每章至少包含1个伏笔
                            10. 情节点需包含场景、冲突、角色发展要素
                            11. json的引号只能使用英文，引号中间的字符串如果出现引号，要转义。
                            示例JSON结构：
                            {
                                "summary": "完整概要",
                                "characterSettings": ["角色设定1", "角色设定2"],
                                "parts": [ 
                                    {
                                        "partTitle": "篇名",
                                        "partSummary": "第一篇概要",
                                        "chapters": [
                                            {
                                                 "title": "章节标题1",
                                                 "plotPoints": ["情节点1", "情节点2"],
                                                 "foreshadowings": ["伏笔1"]
                                            },
                                            {
                                                 "title": "章节标题2",
                                                 "plotPoints": ["情节点1", "情节点2"],
                                                 "foreshadowings": ["伏笔1"]
                                            }]
                                    }
                                ]
                            }""".formatted(summary, totalChapters);

        String conversationId = "outline_" + UUID.randomUUID();

        return parseOutlineResponse(novelSupport.chat(conversationId, prompt),
                totalChapters);
    }

    private NovelOutline parseOutlineResponse(String response, Integer totalChapters) {
        try {
            // 分阶段处理混合内容
            String processedContent = novelSupport.preprocessResponse(response);
            log.debug("预处理后内容：\n{}", processedContent); // 新增日志

            String jsonContent = novelSupport.extractJsonFromMarkdown(processedContent);
            log.debug("提取的JSON内容：\n{}", jsonContent); // 新增日志

            return novelSupport.parseValidatedJson(jsonContent,totalChapters);
        } catch (Exception e) {
            log.error("原始响应内容：\n{}", response); // 记录原始响应
            log.error("解析大纲失败：\n{}", ExceptionUtils.getStackTrace(e));
            throw new NovelGenerationException("大纲解析失败: " + e.getMessage());
        }
    }
}
