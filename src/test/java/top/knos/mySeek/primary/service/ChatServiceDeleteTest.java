package top.knos.mySeek.primary.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.security.domain.User;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatServiceDeleteTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private ChatService service(RedisChatMemory memory) {
        return new ChatService(mock(ChatClient.class), memory,
                mock(ModelProviderManager.class), mock(WebSearchService.class));
    }

    private void loginAs(long userId) {
        User user = new User();
        user.setId(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void deleteChatItemClearsScopedMemoryAndIndexEntry() {
        loginAs(7L);
        RedisChatMemory memory = mock(RedisChatMemory.class);

        service(memory).deleteChatItem("conv-1");

        verify(memory).clear("7:conv-1");
        verify(memory).removeChatItem("7", "conv-1");
    }

    @Test
    void clearChatItemsClearsOnlyCurrentUser() {
        loginAs(7L);
        RedisChatMemory memory = mock(RedisChatMemory.class);

        service(memory).clearChatItems();

        verify(memory).clearConversations("7");
        verify(memory).clearChatItems("7");
    }

    @Test
    void deleteChatItemFallsBackToAnonymousScope() {
        SecurityContextHolder.clearContext();
        RedisChatMemory memory = mock(RedisChatMemory.class);

        service(memory).deleteChatItem("conv-1");

        verify(memory).clear("anonymous:conv-1");
        verify(memory).removeChatItem("anonymous", "conv-1");
    }
}
