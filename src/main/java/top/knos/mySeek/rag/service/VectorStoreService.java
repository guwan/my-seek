package top.knos.mySeek.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import top.knos.mySeek.exception.CustomHttpException;

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
    /** 文档归属用户元数据字段名（字符串形式，供向量检索过滤） */
    private static final String CREATED_BY_METADATA = "created_by";

    @Autowired
    private VectorStore vectorStore;

    /**
     * 文本分块大小
     */
    private static final int CHUNK_SIZE = 1000;

    /** 相似度阈值：低于该相关度的片段不进入回答，避免无关内容干扰 */
    @Value("${rag.similarity-threshold:0.35}")
    private double similarityThreshold = 0.35;

    /** 检索返回片段的最大条数，防止超大 topK 撑爆上下文 */
    @Value("${rag.max-top-k:20}")
    private int maxTopK = 20;

    /** 拼接进 prompt 的参考信息字符预算，防止上下文超限与成本失控 */
    @Value("${rag.prompt-max-chars:8000}")
    private int promptMaxChars = 8000;

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
    public List<Document> searchSimilarDocuments(Long userId, String query, int topK) {
        try {
            log.info("搜索相关文档: userId={}, query={}, topK={}", userId, query, topK);

            Filter.Expression userFilter = new FilterExpressionBuilder()
                    .eq(CREATED_BY_METADATA, String.valueOf(userId))
                    .build();
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(Math.max(1, Math.min(topK, maxTopK)))
                    .similarityThreshold(similarityThreshold)
                    .filterExpression(userFilter)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("搜索完成: 找到 {} 个相关文档", results.size());

            return results;
        } catch (Exception e) {
            log.error("搜索文档失败: query={}", query, e);
            throw new CustomHttpException(describeFailure(e), HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 把底层异常翻译成可读提示。
     * <p>向量检索依赖 Redis 的 RediSearch（Redis Stack）模块，普通 Redis 会返回
     * “Unknown command FT.SEARCH”，这里给出明确的安装指引而不是 500。</p>
     */
    private String describeFailure(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("not found, try pulling")) {
            return "向量检索不可用：本地 Ollama 未安装嵌入模型，请执行 `ollama pull nomic-embed-text`";
        }
        if (message.contains("FT.") || message.contains("Unknown command")) {
            return "向量检索不可用：Redis 缺少 RediSearch 模块（Redis Stack），"
                    + "请使用带搜索模块的 Redis，或设置 spring.ai.vectorstore.redis.initialize-schema=true 后重建索引";
        }
        if (message.contains("Connection refused") || message.contains("Unable to connect")) {
            return "向量检索不可用：无法连接 Redis，请检查 Redis 是否已启动";
        }
        return "搜索文档失败: " + message;
    }

    /**
     * 搜索特定文档内的相关内容
     */
    public List<Document> searchInDocument(Long userId, String documentId, String query, int topK) {
        try {
            log.info("在文档 {} 中搜索: userId={}, query={}, topK={}", documentId, userId, query, topK);

            // 直接在向量层按文档 ID + 归属用户过滤，避免旧的"先全局 topK*2 再过滤"漏召回
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            Filter.Expression expression = builder.and(
                    builder.eq(DOCUMENT_ID_METADATA, documentId),
                    builder.eq(CREATED_BY_METADATA, String.valueOf(userId))
            ).build();

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(Math.max(1, Math.min(topK, maxTopK)))
                    .similarityThreshold(similarityThreshold)
                    .filterExpression(expression)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("文档内搜索完成: 找到 {} 个相关片段", results.size());
            return results;
        } catch (Exception e) {
            log.error("文档内搜索失败: documentId={}, query={}", documentId, query, e);
            throw new RuntimeException("文档内搜索失败: " + e.getMessage(), e);
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
        int used = 0;
        for (int i = 0; i < contextDocuments.size() && used < promptMaxChars; i++) {
            Document doc = contextDocuments.get(i);
            String header = "\n【片段 " + (i + 1) + "】\n";
            int remaining = promptMaxChars - used - header.length();
            if (remaining <= 0) break;
            String text = doc.getText() == null ? "" : doc.getText();
            if (text.length() > remaining) {
                text = text.substring(0, remaining) + "…（已截断）";
            }
            prompt.append(header).append(text).append("\n");
            used += header.length() + text.length();
        }
        
        prompt.append("\n=== 用户问题 ===\n");
        prompt.append(query);
        prompt.append("\n\n请基于上述参考信息回答问题。");
        
        return prompt.toString();
    }
}
