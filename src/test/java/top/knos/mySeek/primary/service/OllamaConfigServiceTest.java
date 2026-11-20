package top.knos.mySeek.primary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.knos.mySeek.exception.TipsException;
import top.knos.mySeek.primary.model.OllamaModelProvider;
import top.knos.mySeek.primary.model.entity.OllamaConfig;
import top.knos.mySeek.primary.model.repository.OllamaConfigRepository;
import top.knos.mySeek.primary.vo.OllamaConfigRequest;
import top.knos.mySeek.primary.vo.OllamaConfigVo;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OllamaConfigServiceTest {

    private OllamaConfigRepository repository;
    private OllamaModelProvider provider;
    private OllamaConfigService service;

    @BeforeEach
    void setUp() {
        repository = mock(OllamaConfigRepository.class);
        provider = mock(OllamaModelProvider.class);
        when(provider.getBaseUrl()).thenReturn("http://localhost:11434");
        when(provider.getDefaultChatModel()).thenReturn("deepseek-r1:8b");
        when(provider.getEmbeddingModel()).thenReturn("nomic-embed-text");
        when(provider.getPullModelStrategy()).thenReturn("NEVER");
        service = new OllamaConfigService(repository, provider);
    }

    private OllamaConfigRequest request() {
        OllamaConfigRequest req = new OllamaConfigRequest();
        req.setBaseUrl("http://localhost:11434/");
        req.setDefaultChatModel("qwen3:8b");
        req.setEmbeddingModel("bge-m3");
        req.setPullModelStrategy("when_missing");
        return req;
    }

    @Test
    void getFallsBackToRuntimeDefaultsWhenNotConfigured() {
        when(repository.findById("default")).thenReturn(Optional.empty());

        OllamaConfigVo vo = service.get();

        assertFalse(vo.isConfigured());
        assertEquals("http://localhost:11434", vo.getBaseUrl());
        assertEquals("deepseek-r1:8b", vo.getDefaultChatModel());
    }

    @Test
    void updatePersistsAndAppliesConfig() {
        when(repository.findById("default")).thenReturn(Optional.empty());
        when(repository.save(any(OllamaConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        OllamaConfigVo vo = service.update(request());

        assertTrue(vo.isConfigured());
        assertEquals("http://localhost:11434", vo.getBaseUrl());
        assertEquals("WHEN_MISSING", vo.getPullModelStrategy());
        verify(provider).reconfigure("http://localhost:11434", "qwen3:8b", "bge-m3", "WHEN_MISSING");
    }

    @Test
    void rejectsInvalidBaseUrl() {
        OllamaConfigRequest req = request();
        req.setBaseUrl("ftp://localhost:11434");
        assertThrows(TipsException.class, () -> service.update(req));
    }
}
