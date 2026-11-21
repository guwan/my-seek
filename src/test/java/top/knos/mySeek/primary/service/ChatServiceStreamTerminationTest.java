package top.knos.mySeek.primary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.primary.vo.ChatRequest;
import top.knos.mySeek.support.vo.WebSearchResult;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatServiceStreamTerminationTest {

    private ChatClient.StreamResponseSpec streamSpec;
    private ChatService service;

    @BeforeEach
    void setUp() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        streamSpec = mock(ChatClient.StreamResponseSpec.class);
        ChatClient.AdvisorSpec advisorSpec = mock(ChatClient.AdvisorSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any())).thenAnswer(invocation -> {
            Consumer<ChatClient.AdvisorSpec> consumer = invocation.getArgument(0);
            consumer.accept(advisorSpec);
            return requestSpec;
        });
        when(advisorSpec.advisors(any(Advisor[].class))).thenReturn(advisorSpec);
        when(advisorSpec.param(anyString(), any())).thenReturn(advisorSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);

        WebSearchService webSearchService = mock(WebSearchService.class);
        when(webSearchService.search(anyString())).thenReturn(new WebSearchResult());

        service = new ChatService(chatClient, mock(RedisChatMemory.class),
                mock(ModelProviderManager.class), webSearchService);
        // 缩短超时以便单测快速覆盖停滞分支
        ReflectionTestUtils.setField(service, "stallTimeoutSeconds", 1L);
    }

    private List<String> stream() {
        return service.chatStream(new ChatRequest("conv-1", "你好"))
                .collectList()
                .block(Duration.ofSeconds(5));
    }

    @Test
    void normalCompletionEmitsMetaContentAndDone() {
        when(streamSpec.content()).thenReturn(Flux.just("你好", "世界"));

        List<String> frames = stream();

        assertNotNull(frames);
        assertTrue(frames.get(0).contains("\"type\":\"meta\""));
        assertTrue(frames.stream().anyMatch(f -> f.contains("\"type\":\"content\"")));
        assertTrue(frames.get(frames.size() - 1).contains("\"type\":\"done\""));
        assertFalse(frames.stream().anyMatch(f -> f.contains("\"type\":\"error\"")));
    }

    @Test
    void stalledUpstreamEmitsDoneInsteadOfHanging() {
        when(streamSpec.content()).thenReturn(Flux.never());

        List<String> frames = stream();

        assertNotNull(frames);
        assertTrue(frames.get(frames.size() - 1).contains("\"type\":\"done\""));
        assertFalse(frames.stream().anyMatch(f -> f.contains("\"type\":\"error\"")));
    }

    @Test
    void upstreamErrorEmitsErrorFrame() {
        when(streamSpec.content()).thenReturn(Flux.error(new RuntimeException("boom")));

        List<String> frames = stream();

        assertNotNull(frames);
        assertTrue(frames.get(frames.size() - 1).contains("\"type\":\"error\""));
    }
}
