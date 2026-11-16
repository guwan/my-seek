package top.knos.mySeek.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量存储服务
 * 负责文档分块、向量化和存储
 */
@Slf4j
@Service
public class VectorStoreService {

    /** 文档 ID 元数据字段名 */
    private static final String DOCUMENT_ID_METADATA = "document_id";

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 文本分块大小
     */
    private static final int CHUNK_SIZE = 1000;

    /**
     * 分块重叠大小
     */
    private static final int CHUNK_OVERLAP = 200;

    /**
     * 将文档内容分块并存储到向量数据库
     */
    public int storeDocument(String documentId, String content, Map<String, Object> metadata) {
        try {
            log.info("开始处理文档: {}, 内容长度: {}", documentId, content.length());

            // 1. 创建 Document 对象
            Document document = new Document(content, metadata);

            // 2. 使用 TokenTextSplitter 进行分块
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(CHUNK_SIZE)
                    .withMinChunkSizeChars(5)
                    .withMinChunkLengthToEmbed(5)
                    .withMaxNumChunks(10000)
                    .withKeepSeparator(true)
                    .build();

            List<Document> chunks = splitter.split(List.of(document));
            log.info("文档分块完成: {} 块", chunks.size());

            // 3. 为每个分块添加元数据
            List<Document> documentsWithMetadata = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                Map<String, Object> chunkMetadata = new HashMap<>(chunk.getMetadata());
                chunkMetadata.put(DOCUMENT_ID_METADATA, documentId);
                chunkMetadata.put("chunk_index", i);
                chunkMetadata.put("chunk_total", chunks.size());
                
                Document docWithMetadata = new Document(chunk.getText(), chunkMetadata);
                documentsWithMetadata.add(docWithMetadata);
            }

            // 4. 存储到向量数据库
            vectorStore.add(documentsWithMetadata);
            log.info("文档向量存储完成: {}, 共 {} 块", documentId, chunks.size());

            return chunks.size();
        } catch (Exception e) {
            log.error("文档向量存储失败: {}", documentId, e);
            throw new RuntimeException("文档向量存储失败: " + e.getMessage());
        }
    }

    /**
     * 搜索相关文档
     */
    public List<Document> searchSimilarDocuments(String query, int topK) {
        try {
            log.info("搜索相关文档: query={}, topK={}", query, topK);

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("搜索完成: 找到 {} 个相关文档", results.size());

            return results;
        } catch (Exception e) {
            log.error("搜索文档失败: {}", query, e);
            throw new RuntimeException("搜索文档失败: " + e.getMessage());
        }
    }

    /**
     * 搜索特定文档内的相关内容
     */
    public List<Document> searchInDocument(String documentId, String query, int topK) {
        try {
            log.info("在文档 {} 中搜索: query={}, topK={}", documentId, query, topK);

            // 先搜索所有相关文档
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK * 2) // 多搜索一些，然后过滤
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            // 过滤出指定文档的结果
            List<Document> filteredResults = results.stream()
                    .filter(doc -> documentId.equals(doc.getMetadata().get("document_id")))
                    .limit(topK)
                    .toList();

            log.info("文档内搜索完成: 找到 {} 个相关片段", filteredResults.size());
            return filteredResults;
        } catch (Exception e) {
            log.error("文档内搜索失败: documentId={}, query={}", documentId, query, e);
            throw new RuntimeException("文档内搜索失败: " + e.getMessage());
        }
    }

    /**
     * 删除文档的所有向量。
     * <p>按写入时附加的 {@code document_id} 元数据过滤删除（RedisVectorStore 支持按元数据过滤删除）。</p>
     */
    public void deleteDocumentVectors(String documentId) {
        try {
            log.info("删除文档向量: {}", documentId);
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            Filter.Expression expression = builder.eq(DOCUMENT_ID_METADATA, documentId).build();
            vectorStore.delete(expression);
            log.info("文档向量删除完成: {}", documentId);
        } catch (Exception e) {
            log.error("删除文档向量失败: {}", documentId, e);
            throw new IllegalStateException("删除文档向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 RAG 提示词
     */
    public String buildRagPrompt(String query, List<Document> contextDocuments) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("基于以下参考信息回答问题。如果参考信息不足以回答问题，请明确说明。\n\n");
        
        prompt.append("=== 参考信息 ===\n");
        for (int i = 0; i < contextDocuments.size(); i++) {
            Document doc = contextDocuments.get(i);
            prompt.append("\n【片段 ").append(i + 1).append("】\n");
            prompt.append(doc.getText());
            prompt.append("\n");
        }
        
        prompt.append("\n=== 用户问题 ===\n");
        prompt.append(query);
        prompt.append("\n\n请基于上述参考信息回答问题。");
        
        return prompt.toString();
    }
}
