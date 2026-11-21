package top.knos.mySeek.primary.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.knos.mySeek.primary.vo.NovelChapter;
import top.knos.mySeek.primary.vo.NovelOutline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@Service
@Slf4j
public class NovelStorageService {
    private final Path basePath;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public NovelStorageService(
            @Value("${novel.storage.path:${app.work-dir:${user.home}/MySeek}/novels}") String basePath) {
        this.basePath = Path.of(basePath);
    }

    public void saveOutline(NovelOutline outline) throws JacksonException {
        String content = objectMapper.writeValueAsString(outline);

        String fileName = "小说概要.json";
        saveToFile(fileName, content);
        log.info("写入概要文件：{}",fileName);
    }

    public NovelOutline loadOutline() throws JacksonException {

        String fileName = "小说概要.json";
        log.debug("读取历史文件概要文件：{}",fileName);
        String content = readFile(fileName);
        if(StringUtils.isEmpty(content)){
            log.debug("不存在历史文件概要文件");
            return null;
        }
        return objectMapper.readValue(content, NovelOutline.class);
    }

    public void saveChapter(NovelChapter chapter) {
        String content = "# 第" + chapter.chapterNumber() + "章 " + chapter.title() 
                        + "\n\n" + chapter.content();

        String safeTitle = sanitizeFilename(chapter.title());
        String fileName = String.format("章节/%03d_%s.md", chapter.chapterNumber(),
                safeTitle);
        saveToFile(fileName, content);
        log.info("写入章节文件：{}",fileName);
    }
    private String sanitizeFilename(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ");
    }

    private void saveToFile(String relativePath, String content) {
        try {
            Path fullPath = basePath.resolve(relativePath);
            Files.createDirectories(fullPath.getParent());
            Files.writeString(fullPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败", e);
        }
    }


    private String readFile(String relativePath) {
        try {
            Path fullPath = basePath.resolve(relativePath);
            return Files.readString(fullPath, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败", e);
        }
    }
}
