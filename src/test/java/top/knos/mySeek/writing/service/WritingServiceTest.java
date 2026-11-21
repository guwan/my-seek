package top.knos.mySeek.writing.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import top.knos.mySeek.writing.entity.WritingNovel;
import top.knos.mySeek.writing.repository.WritingNovelRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WritingServiceTest {

    private WritingNovelRepository repository;
    private WritingService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository = mock(WritingNovelRepository.class);
        objectMapper = JsonMapper.builder().build();
        service = new WritingService(repository);
    }

    private JsonNode payload(String id, String title) throws Exception {
        return objectMapper.readTree("{\"id\":\"" + id + "\",\"title\":\"" + title + "\",\"volumes\":[]}");
    }

    @Test
    void saveNovelCreatesOwnedRecord() throws Exception {
        JsonNode payload = payload("novel-1", "我的小说");
        when(repository.findById("novel-1")).thenReturn(Optional.empty());

        JsonNode result = service.saveNovel(42L, payload);

        assertEquals(payload, result);
        verify(repository).save(any(WritingNovel.class));
    }

    @Test
    void saveNovelUpdatesExistingOwnedRecord() throws Exception {
        WritingNovel existing = new WritingNovel();
        existing.setId("novel-1");
        existing.setUserId(42L);
        existing.setTitle("旧标题");
        existing.setContentJson("{}");
        when(repository.findById("novel-1")).thenReturn(Optional.of(existing));

        service.saveNovel(42L, payload("novel-1", "新标题"));

        assertEquals("新标题", existing.getTitle());
        assertEquals(42L, existing.getUserId());
        verify(repository).save(existing);
    }

    @Test
    void saveNovelRejectsOtherUsersRecord() throws Exception {
        WritingNovel existing = new WritingNovel();
        existing.setId("novel-1");
        existing.setUserId(7L);
        when(repository.findById("novel-1")).thenReturn(Optional.of(existing));

        assertThrows(AccessDeniedException.class,
                () -> service.saveNovel(42L, payload("novel-1", "窃取")));
        verify(repository, never()).save(any());
    }

    @Test
    void listNovelsParsesStoredJson() {
        WritingNovel entity = new WritingNovel();
        entity.setId("novel-1");
        entity.setTitle("标题");
        entity.setContentJson("{\"id\":\"novel-1\",\"title\":\"标题\",\"volumes\":[]}");
        when(repository.findByUserIdOrderByCreatedAtDesc(42L)).thenReturn(List.of(entity));

        List<JsonNode> novels = service.listNovels(42L);

        assertEquals(1, novels.size());
        assertEquals("标题", novels.get(0).path("title").asText());
    }

    @Test
    void deleteNovelSkipsMissingAndRejectsForeign() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        service.deleteNovel(42L, "missing");
        verify(repository, never()).delete(any());

        WritingNovel foreign = new WritingNovel();
        foreign.setId("foreign");
        foreign.setUserId(7L);
        when(repository.findById("foreign")).thenReturn(Optional.of(foreign));
        assertThrows(AccessDeniedException.class, () -> service.deleteNovel(42L, "foreign"));
    }
}
