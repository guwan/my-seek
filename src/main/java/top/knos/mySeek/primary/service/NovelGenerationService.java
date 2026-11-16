package top.knos.mySeek.primary.service;

import tools.jackson.core.JacksonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import top.knos.mySeek.primary.vo.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class NovelGenerationService {
    private final ChatClient chatClient;
    private final NovelStorageService storageService;
    private final ChatService chatService;// 新增配置参数
    private final NovelOutlineService novelOutlineService;
    private final NovelChapterService novelChapterService;

    // 生成流程控制
    public void generateNovel(String storySummary, Integer totalChapters) throws JacksonException {
        //先看有没有历史记录
        NovelOutline outline = novelOutlineService.generateNovel(storySummary, totalChapters);

        novelChapterService.generateChapter(outline, totalChapters);

    }
}
