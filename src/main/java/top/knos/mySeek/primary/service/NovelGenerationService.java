package top.knos.mySeek.primary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import top.knos.mySeek.exception.InvalidOutlineException;
import top.knos.mySeek.exception.NovelGenerationException;
import top.knos.mySeek.primary.vo.*;
import top.knos.mySeek.utils.JsonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    public void generateNovel(String storySummary, Integer totalChapters) throws JsonProcessingException {
        //先看有没有历史记录
        NovelOutline outline = novelOutlineService.generateNovel(storySummary, totalChapters);

        novelChapterService.generateChapter(outline, totalChapters);

    }
}