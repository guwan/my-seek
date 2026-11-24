package top.knos.mySeek.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.mock.web.MockMultipartFile;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.rag.entity.KnowledgeDocument;
import top.knos.mySeek.rag.repository.KnowledgeDocumentRepository;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KnowledgeAsyncTest {

    @TempDir
    Path tempDir;

    private KnowledgeDocumentRepository repository;
    private DocumentParserService parser;
    private VectorStoreService vectorStoreService;
    private KnowledgeProcessingService processingService;
    private KnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeDocumentRepository.class);
        parser = mock(DocumentParserService.class);
        vectorStoreService = mock(VectorStoreService.class);
        processingService = mock(KnowledgeProcessingService.class);
        service = new KnowledgeBaseService(repository, parser, vectorStoreService,
                mock(ChatClient.class), mock(ModelProviderManager.class), processingService,
                tempDir.resolve("uploads").toString(), "50MB");
        when(parser.getFileExtension("note.txt")).thenReturn("txt");
        when(parser.isSupportedFileType("note.txt")).thenReturn(true);
        when(repository.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> {
            KnowledgeDocument doc = invocation.getArgument(0);
            if (doc.getId() == null) doc.setId("doc-1");
            return doc;
        });
    }

    @Test
    void uploadReturnsPendingAndSchedulesBackgroundTask() {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

        KnowledgeDocument result = service.uploadDocument(file, 1L);

        assertEquals(KnowledgeDocument.DocumentStatus.PENDING, result.getStatus());
        assertNotNull(result.getStoragePath());
        assertEquals(0, result.getRetryCount());
        verify(processingService).processAsync(eq("doc-1"), eq(1L));
        verify(vectorStoreService, never()).storeDocument(anyString(), anyString(), any());
    }

    @Test
    void retryOnlyFailedDocumentsAndIncrementsCounter() {
        KnowledgeDocument failed = doc(KnowledgeDocument.DocumentStatus.FAILED, 0);
        when(repository.findByIdAndCreatedBy("doc-1", 1L)).thenReturn(Optional.of(failed));

        KnowledgeDocument result = service.retryDocument("doc-1", 1L);

        assertEquals(KnowledgeDocument.DocumentStatus.PENDING, result.getStatus());
        assertEquals(1, result.getRetryCount());
        assertNull(result.getErrorMessage());
        verify(processingService).processAsync("doc-1", 1L);

        failed.setStatus(KnowledgeDocument.DocumentStatus.COMPLETED);
        assertThrows(IllegalStateException.class, () -> service.retryDocument("doc-1", 1L));
    }

    @Test
    void workerMarksDocumentFailedWhenFileIsMissing() {
        KnowledgeDocument document = doc(KnowledgeDocument.DocumentStatus.PROCESSING, 0);
        document.setStoragePath(tempDir.resolve("missing.txt").toString());
        when(repository.findByIdAndCreatedBy("doc-1", 1L)).thenReturn(Optional.of(document));
        KnowledgeProcessingService worker = new KnowledgeProcessingService(
                repository, parser, vectorStoreService, mock(Executor.class));

        worker.processAsync("doc-1", 1L);

        assertEquals(KnowledgeDocument.DocumentStatus.FAILED, document.getStatus());
        assertNotNull(document.getErrorMessage());
        assertFalse(worker.isActive("doc-1"));
        verify(vectorStoreService).deleteDocumentVectors("doc-1");
    }

    private KnowledgeDocument doc(KnowledgeDocument.DocumentStatus status, int retryCount) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId("doc-1");
        document.setName("stored.txt");
        document.setOriginalName("note.txt");
        document.setType("txt");
        document.setSize(5L);
        document.setStatus(status);
        document.setRetryCount(retryCount);
        document.setCreatedBy(1L);
        return document;
    }
}
