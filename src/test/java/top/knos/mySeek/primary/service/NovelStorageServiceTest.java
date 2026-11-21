package top.knos.mySeek.primary.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.knos.mySeek.primary.vo.NovelChapter;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NovelStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void saveChapterWritesInsideConfiguredDirectory() throws Exception {
        Path workDir = tempDir.resolve("novels");
        NovelStorageService storage = new NovelStorageService(workDir.toString());

        storage.saveChapter(new NovelChapter(1, "测试章节", "正文内容"));

        Path chapterFile = workDir.resolve("章节/001_测试章节.md");
        assertTrue(Files.exists(chapterFile));
        assertTrue(Files.readString(chapterFile).contains("正文内容"));
    }
}
