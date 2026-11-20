package top.knos.mySeek.primary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.primary.model.entity.AiProviderConfig;
import top.knos.mySeek.primary.model.repository.AiProviderConfigRepository;
import top.knos.mySeek.primary.vo.AiProviderRequest;
import top.knos.mySeek.primary.vo.AiProviderVo;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiProviderConfigServiceTest {

    private AiProviderConfigRepository repository;
    private ModelProviderManager providerManager;
    private AiProviderConfigService service;

    @BeforeEach
    void setUp() {
        repository = mock(AiProviderConfigRepository.class);
        providerManager = mock(ModelProviderManager.class);
        service = new AiProviderConfigService(repository, providerManager);
        ReflectionTestUtils.setField(service, "encryptSecret", "unit-test-secret");
    }

    private AiProviderRequest request(String name) {
        AiProviderRequest req = new AiProviderRequest();
        req.setId("p1");
        req.setName(name);
        req.setDisplayName("DeepSeek 官方");
        req.setBaseUrl("https://api.deepseek.com");
        req.setApiKey("sk-1234567890abcdef");
        req.setModels(List.of("deepseek-chat"));
        req.setEnabled(true);
        return req;
    }

    private AiProviderConfig existing() {
        AiProviderConfig config = new AiProviderConfig();
        config.setId("p1");
        config.setName("deepseek");
        config.setDisplayName("DeepSeek 官方");
        config.setBaseUrl("https://api.deepseek.com/v1");
        config.setApiKey("encrypted");
        config.setModelsJson("[\"deepseek-chat\"]");
        config.setEnabled(true);
        return config;
    }

    @Test
    void createRegistersProviderAndMasksKey() {
        when(repository.findByName("deepseek")).thenReturn(Optional.empty());
        when(repository.save(any(AiProviderConfig.class))).thenAnswer(inv -> {
            AiProviderConfig saved = inv.getArgument(0);
            saved.setId("p1");
            return saved;
        });

        AiProviderVo vo = service.create(request("deepseek"));

        assertEquals("p1", vo.getId());
        assertTrue(vo.isApiKeyConfigured());
        assertTrue(vo.getApiKeyMasked().startsWith("sk-1****"));
        assertTrue(vo.getApiKeyMasked().endsWith("cdef"));
        verify(providerManager).registerProvider(eq("deepseek"), any());
    }

    @Test
    void createRejectsBuiltInName() {
        assertThrows(IllegalArgumentException.class, () -> service.create(request("ollama")));
        verify(repository, never()).save(any());
    }

    @Test
    void updateAllowsUnchangedNameWhenIdMatches() {
        when(repository.findById("p1")).thenReturn(Optional.of(existing()));
        when(repository.findByName("deepseek")).thenReturn(Optional.of(existing()));
        when(repository.save(any(AiProviderConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AiProviderVo vo = service.update("p1", request("deepseek"));

        assertEquals("p1", vo.getId());
        verify(providerManager).unregisterProvider("deepseek");
        verify(providerManager).registerProvider(eq("deepseek"), any());
    }

    @Test
    void deleteUnregistersProvider() {
        when(repository.findById("p1")).thenReturn(Optional.of(existing()));

        service.delete("p1");

        verify(providerManager).unregisterProvider("deepseek");
        verify(repository).delete(any(AiProviderConfig.class));
    }

    @Test
    void toggleOffUnregistersProvider() {
        when(repository.findById("p1")).thenReturn(Optional.of(existing()));
        when(repository.save(any(AiProviderConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        service.toggle("p1", false);

        verify(providerManager).unregisterProvider("deepseek");
    }
}
