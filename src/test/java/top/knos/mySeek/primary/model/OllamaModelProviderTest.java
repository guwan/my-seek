package top.knos.mySeek.primary.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OllamaModelProviderTest {

    @Test
    void reconfigureUpdatesRuntimeSettingsAndNormalizesBaseUrl() {
        OllamaModelProvider provider = new OllamaModelProvider(
                "http://localhost:11434/", "deepseek-r1:8b", "nomic-embed-text", "NEVER");

        assertEquals("http://localhost:11434", provider.getBaseUrl());
        assertEquals("deepseek-r1:8b", provider.getDefaultChatModel());

        provider.reconfigure("http://127.0.0.1:11435/", "qwen3:8b", "bge-m3", "ALWAYS");

        assertEquals("http://127.0.0.1:11435", provider.getBaseUrl());
        assertEquals("qwen3:8b", provider.getDefaultChatModel());
        assertEquals("bge-m3", provider.getEmbeddingModel());
        assertEquals("ALWAYS", provider.getPullModelStrategy());
    }
}
