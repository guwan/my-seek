package top.knos.mySeek.rag.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorStoreServiceTest {

    @Test
    void buildRagPromptTruncatesOversizedContextWithinBudget() {
        VectorStoreService service = new VectorStoreService();
        String huge = "A".repeat(20000);

        String prompt = service.buildRagPrompt("问题", List.of(new Document(huge)));

        assertTrue(prompt.length() < 9000);
        assertTrue(prompt.contains("已截断"));
        assertTrue(prompt.contains("问题"));
    }

    @Test
    void buildRagPromptKeepsNormalContextIntact() {
        VectorStoreService service = new VectorStoreService();

        String prompt = service.buildRagPrompt("问题", List.of(new Document("短片段内容")));

        assertTrue(prompt.contains("短片段内容"));
        assertFalse(prompt.contains("已截断"));
    }

    @Test
    void buildContextTextContainsOnlySnippetsWithoutQuestionInstruction() {
        VectorStoreService service = new VectorStoreService();

        String context = service.buildContextText(List.of(new Document("短片段内容")));

        assertTrue(context.contains("=== 参考信息 ==="));
        assertTrue(context.contains("短片段内容"));
        assertFalse(context.contains("用户问题"));
    }
}
