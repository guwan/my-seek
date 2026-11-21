package top.knos.mySeek.writing.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.writing.service.WritingService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WritingControllerTest {

    private WritingService writingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(42L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        writingService = mock(WritingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WritingController(writingService)).build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveNovelAcceptsJsonNodeBodyWithJackson3Converter() throws Exception {
        when(writingService.saveNovel(eq(42L), any(JsonNode.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        mockMvc.perform(put("/api/writing/novels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"novel-1\",\"title\":\"我的小说\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("novel-1"));
    }

    @Test
    void listNovelsSerializesJsonNodesWithJackson3Converter() throws Exception {
        when(writingService.listNovels(42L)).thenReturn(List.of());

        mockMvc.perform(get("/api/writing/novels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
