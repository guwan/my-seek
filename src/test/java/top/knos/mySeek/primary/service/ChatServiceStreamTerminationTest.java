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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceStreamTerminationTest {

    private ChatClient.StreamResponseSpec streamSpec;
    private RedisChatMemory memory;
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

        memory = mock(RedisChatMemory.class);
        service = new ChatService(chatClient, memory,
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

    @Test
    void cancellationPersistsPartialReply() {
        // 发出一个 chunk 后保持流不结束，take(1) 触发取消
        when(streamSpec.content()).thenReturn(Flux.just("已生成的部分内容").concatWith(Flux.never()));

        // 需要消费到 content 帧（meta 是第一段），才会进入带 doOnCancel 的 framedContent
        List<String> frames = service.chatStream(new ChatRequest("conv-1", "你好"))
                .take(2)
                .collectList()
                .block(Duration.ofSeconds(5));

        assertNotNull(frames);
        assertTrue(frames.stream().anyMatch(f -> f.contains("\"type\":\"content\"")));
        verify(memory).add(anyString(), anyList());
    }
}
