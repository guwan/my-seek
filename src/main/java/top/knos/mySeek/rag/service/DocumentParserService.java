package top.knos.mySeek.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * 文档解析服务
 * 使用 Apache Tika 解析各种格式的文档
 */
@Slf4j
@Service
public class DocumentParserService {

    /** 单个文档解析出的最大字符数，避免超大文件导致 OOM */
    private static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt", "md", "markdown",
            "ppt", "pptx", "xls", "xlsx", "csv", "json", "xml", "html", "htm"
    );

    private final Tika tika = new Tika();

    /**
     * 解析文档内容
     */
    public String parseDocument(MultipartFile file) {
        // Tika 4.x 要求使用 TikaInputStream
        try (TikaInputStream inputStream = TikaInputStream.get(file.getInputStream())) {
            // 设置最大输出长度，避免异常大文件撑爆内存
            BodyContentHandler handler = new BodyContentHandler(MAX_CONTENT_LENGTH);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            Parser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata, context);

            String content = handler.toString();
            log.info("成功解析文档: {}, 内容长度: {}", file.getOriginalFilename(), content.length());
            return content;
        } catch (Exception e) {
            log.error("解析文档失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检测文件类型
     */
    public String detectFileType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String mimeType = tika.detect(inputStream);
            log.debug("检测到文件类型: {} -> {}", file.getOriginalFilename(), mimeType);
            return mimeType;
        } catch (IOException e) {
            log.error("检测文件类型失败: {}", file.getOriginalFilename(), e);
            return "application/octet-stream";
        }
    }

    /**
     * 获取文件扩展名
     */
    public String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 检查是否支持的文件类型
     */
    public boolean isSupportedFileType(String filename) {
        return SUPPORTED_EXTENSIONS.contains(getFileExtension(filename));
    }

    /**
     * 获取支持的文件类型列表
     */
    public String[] getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS.toArray(new String[0]);
    }
}
