package top.knos.mySeek.primary.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelProviderManagerTest {

    private ModelProvider provider;
    private ChatClient chatClient;
    private ModelProviderManager manager;

    @BeforeEach
    void setUp() {
        provider = mock(ModelProvider.class);
        chatClient = mock(ChatClient.class);
        when(provider.getProviderName()).thenReturn("fake");
        when(provider.isAvailable()).thenReturn(true);
        when(provider.getAvailableModels()).thenReturn(List.of("model-a"));
        when(provider.createChatClient(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(chatClient);
        manager = new ModelProviderManager(List.of(provider));
    }

    @Test
    void modelIdentifierSplitsOnlyOnFirstColon() {
        assertEquals(chatClient, manager.createChatClient("fake:model:with:colons", false));
        verify(provider).createChatClient("model:with:colons", false);
    }

    @Test
    void unknownProviderIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> manager.createChatClient("missing:model"));
    }

    @Test
    void unavailableProviderIsRejected() {
        when(provider.isAvailable()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> manager.createChatClient("fake:model"));
    }

    @Test
    void defaultChatClientForwardsThinkingFlag() {
        manager.registerProvider("ollama", provider);
        when(provider.createChatClient(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(true))).thenReturn(chatClient);

        assertEquals(chatClient, manager.getDefaultChatClient(true));

        verify(provider).createChatClient(null, true);
    }

    @Test
    void builtInProviderCannotBeUnregistered() {
        manager.registerProvider("ollama", provider);
        manager.unregisterProvider("ollama");
        assertTrue(manager.getAllProviders().containsKey("ollama"));
    }

    @Test
    void getAllAvailableModelsSkipsUnavailableProviders() {
        when(provider.isAvailable()).thenReturn(false);
        Map<String, List<String>> models = manager.getAllAvailableModels();
        assertTrue(models.isEmpty());
    }
}
