package top.knos.mySeek.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.rag.entity.KnowledgeDocument;
import top.knos.mySeek.rag.repository.KnowledgeDocumentRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseIsolationTest {

    private KnowledgeDocumentRepository repository;
    private VectorStoreService vectorStoreService;
    private KnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeDocumentRepository.class);
        DocumentParserService parser = mock(DocumentParserService.class);
        vectorStoreService = mock(VectorStoreService.class);
        service = new KnowledgeBaseService(repository, parser, vectorStoreService,
                mock(ChatClient.class), mock(ModelProviderManager.class),
                mock(KnowledgeProcessingService.class),
                "uploads/documents/", "50MB");
    }

    private KnowledgeDocument doc(String id, Long owner) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setName("doc-" + id);
        document.setType("txt");
        document.setSize(1L);
        document.setStatus(KnowledgeDocument.DocumentStatus.COMPLETED);
        document.setCreatedBy(owner);
        return document;
    }

    @Test
    void listPageQueriesByCurrentUser() {
        Page<KnowledgeDocument> page = new PageImpl<>(List.of(doc("d1", 1L)));
        when(repository.findByCreatedByOrderByCreatedAtDesc(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<KnowledgeDocument> result = service.getDocumentsPage(1L, 0, 10, null);

        assertEquals(1, result.getContent().size());
        verify(repository).findByCreatedByOrderByCreatedAtDesc(eq(1L), any(Pageable.class));
    }

    @Test
    void completedDocumentsQueriesByCurrentUser() {
        when(repository.findByCreatedByAndStatusOrderByCreatedAtDesc(1L, KnowledgeDocument.DocumentStatus.COMPLETED))
                .thenReturn(List.of(doc("d1", 1L)));

        service.getCompletedDocuments(1L);

        verify(repository).findByCreatedByAndStatusOrderByCreatedAtDesc(1L, KnowledgeDocument.DocumentStatus.COMPLETED);
    }

    @Test
    void deleteForeignDocumentIsRejected() {
        when(repository.findByIdAndCreatedBy("d2", 1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.deleteDocument("d2", 1L));

        verify(vectorStoreService, never()).deleteDocumentVectors(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteOwnDocumentDeletesVectorsAndRecord() {
        KnowledgeDocument own = doc("d1", 1L);
        when(repository.findByIdAndCreatedBy("d1", 1L)).thenReturn(Optional.of(own));

        service.deleteDocument("d1", 1L);

        verify(vectorStoreService).deleteDocumentVectors("d1");
        verify(repository).delete(own);
    }

    @Test
    void queryForeignDocumentIsRejected() {
        when(repository.findByIdAndCreatedBy("d2", 1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.answerBasedOnDocument(1L, "d2", "问题", 5, null));

        verify(vectorStoreService, never()).searchInDocument(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void globalQueryFiltersVectorsByUser() {
        when(vectorStoreService.searchSimilarDocuments(eq(1L), eq("问题"), eq(5)))
                .thenReturn(List.of());

        service.answerBasedOnKnowledge(1L, "问题", 5, null);

        verify(vectorStoreService).searchSimilarDocuments(1L, "问题", 5);
    }
}
