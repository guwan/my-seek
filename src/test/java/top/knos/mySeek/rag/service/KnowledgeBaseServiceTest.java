package top.knos.mySeek.rag.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeBaseServiceTest {

    @Test
    void stripThinkingRemovesClosedAndUnclosedBlocks() {
        String text = "前置<think>内部推理</think>最终答案";
        assertEquals("前置最终答案", KnowledgeBaseService.stripThinking(text));

        String unclosed = "前缀<thinking>仍在推理";
        assertEquals("前缀", KnowledgeBaseService.stripThinking(unclosed));
    }

    @Test
    void stripThinkingHandlesBlankInput() {
        assertEquals("", KnowledgeBaseService.stripThinking(null));
        assertEquals("", KnowledgeBaseService.stripThinking("   "));
    }
}
