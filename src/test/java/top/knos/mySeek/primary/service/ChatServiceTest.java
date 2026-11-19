package top.knos.mySeek.primary.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.primary.vo.ChatRequest;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.support.SecurityUtils;
import top.knos.mySeek.support.vo.ChatInfoVo;
import top.knos.mySeek.support.vo.WebSearchResult;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void chatStreamUsesScopeCapturedOnRequestThread() {
        User user = new User();
        user.setId(42L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        ChatClient chatClient = mock(ChatClient.class);
        RedisChatMemory memory = mock(RedisChatMemory.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamSpec = mock(ChatClient.StreamResponseSpec.class);
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
        // 让完成回调脱离请求线程执行，模拟 Reactor 线程上没有 SecurityContext 的场景
        when(streamSpec.content()).thenReturn(Flux.just("回复内容").subscribeOn(Schedulers.boundedElastic()));

        WebSearchService webSearchService = mock(WebSearchService.class);
        when(webSearchService.search(anyString())).thenReturn(new WebSearchResult());

        ChatService service = new ChatService(
                chatClient, memory, mock(ModelProviderManager.class), webSearchService);

        Flux<String> stream = service.chatStream(new ChatRequest("conv-1", "你好"));
        SecurityContextHolder.clearContext();

        List<String> frames = stream.collectList().block();
        assertEquals(3, frames.size());
        assertTrue(frames.get(0).startsWith(ChatService.STREAM_FRAME_PREFIX + "{\"type\":\"meta\""));
        assertTrue(frames.get(1).contains("\"type\":\"content\""));
        assertTrue(frames.get(1).contains("回复内容"));
        assertTrue(frames.get(2).startsWith(ChatService.STREAM_FRAME_PREFIX + "{\"type\":\"done\""));

        verify(advisorSpec).param(ChatMemory.CONVERSATION_ID, "42:conv-1");
        verify(memory).addChatItem(eq("42"), any(ChatInfoVo.class));
    }

    @Test
    void securityUtilsIsNullSafeWithoutAuthentication() {
        SecurityContextHolder.clearContext();
        assertNull(SecurityUtils.getUser());
        assertFalse(SecurityUtils.currentIsAdmin());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));
        assertNull(SecurityUtils.getUser());
        assertFalse(SecurityUtils.currentIsAdmin());
    }
}
