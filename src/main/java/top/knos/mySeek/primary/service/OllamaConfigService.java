package top.knos.mySeek.primary.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ai.ollama.api.OllamaApi;
import top.knos.mySeek.exception.TipsException;
import top.knos.mySeek.primary.model.OllamaModelProvider;
import top.knos.mySeek.primary.model.entity.OllamaConfig;
import top.knos.mySeek.primary.model.repository.OllamaConfigRepository;
import top.knos.mySeek.primary.vo.OllamaConfigRequest;
import top.knos.mySeek.primary.vo.OllamaConfigVo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 内置 Ollama 配置：数据库持久化 + 运行时热更新。
 * <p>base-url 与默认聊天模型立即生效；embedding 模型与拉取策略需要重启服务后生效。</p>
 */
@Service
@Slf4j
public class OllamaConfigService {

    private static final String CONFIG_ID = "default";
    private static final List<String> PULL_STRATEGIES = List.of("NEVER", "ALWAYS", "WHEN_MISSING");

    private final OllamaConfigRepository repository;
    private final OllamaModelProvider ollamaProvider;

    public OllamaConfigService(OllamaConfigRepository repository, OllamaModelProvider ollamaProvider) {
        this.repository = repository;
        this.ollamaProvider = ollamaProvider;
    }

    /** 启动后把已保存的配置应用到运行时提供者 */
    @PostConstruct
    public void load() {
        try {
            Optional<OllamaConfig> saved = repository.findById(CONFIG_ID);
            saved.ifPresent(this::apply);
        } catch (Exception e) {
            log.warn("加载 Ollama 配置失败，继续使用环境变量默认值: {}", e.getMessage());
        }
    }

    public OllamaConfigVo get() {
        try {
            Optional<OllamaConfig> saved = repository.findById(CONFIG_ID);
            if (saved.isPresent()) return toVo(saved.get(), true);
        } catch (Exception e) {
            log.warn("读取 Ollama 配置失败，返回运行时默认值: {}", e.getMessage());
        }
        return currentRuntimeConfig();
    }

    @Transactional
    public OllamaConfigVo update(OllamaConfigRequest req) {
        validateBaseUrl(req.getBaseUrl());
        String strategy = normalizeStrategy(req.getPullModelStrategy());

        OllamaConfig config = repository.findById(CONFIG_ID).orElseGet(OllamaConfig::new);
        config.setId(CONFIG_ID);
        config.setBaseUrl(normalizeBaseUrl(req.getBaseUrl()));
        config.setDefaultChatModel(req.getDefaultChatModel().trim());
        config.setEmbeddingModel(req.getEmbeddingModel().trim());
        config.setPullModelStrategy(strategy);
        config = repository.save(config);

        apply(config);
        return toVo(config, true);
    }

    /** 用请求中的地址拉取在线模型列表，不修改当前配置 */
    public List<String> test(OllamaConfigRequest req) {
        validateBaseUrl(req.getBaseUrl());
        OllamaApi api = OllamaApi.builder().baseUrl(normalizeBaseUrl(req.getBaseUrl())).build();
        try {
            List<String> models = new ArrayList<>();
            api.listModels().models().forEach(model -> {
                if (model.name() != null) models.add(model.name());
            });
            return models;
        } catch (Exception e) {
            throw new TipsException("无法连接 Ollama：" + e.getMessage(), 400);
        }
    }

    private OllamaConfigVo currentRuntimeConfig() {
        OllamaConfigVo vo = new OllamaConfigVo();
        vo.setBaseUrl(ollamaProvider.getBaseUrl());
        vo.setDefaultChatModel(ollamaProvider.getDefaultChatModel());
        vo.setEmbeddingModel(ollamaProvider.getEmbeddingModel());
        vo.setPullModelStrategy(ollamaProvider.getPullModelStrategy());
        vo.setConfigured(false);
        return vo;
    }

    private void apply(OllamaConfig config) {
        ollamaProvider.reconfigure(config.getBaseUrl(), config.getDefaultChatModel(),
                config.getEmbeddingModel(), config.getPullModelStrategy());
    }

    private OllamaConfigVo toVo(OllamaConfig config, boolean configured) {
        OllamaConfigVo vo = new OllamaConfigVo();
        vo.setBaseUrl(config.getBaseUrl());
        vo.setDefaultChatModel(config.getDefaultChatModel());
        vo.setEmbeddingModel(config.getEmbeddingModel());
        vo.setPullModelStrategy(config.getPullModelStrategy());
        vo.setConfigured(configured);
        vo.setUpdatedAt(config.getUpdatedAt() == null ? null : config.getUpdatedAt().toString());
        return vo;
    }

    private void validateBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new TipsException("base-url 不能为空", 400);
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new TipsException("base-url 必须是 http 或 https 地址", 400);
            }
            if (uri.getHost() == null) {
                throw new TipsException("base-url 缺少主机名", 400);
            }
        } catch (URISyntaxException e) {
            throw new TipsException("base-url 格式不正确", 400);
        }
    }

    private String normalizeBaseUrl(String url) {
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) return "NEVER";
        String upper = strategy.trim().toUpperCase();
        if (!PULL_STRATEGIES.contains(upper)) {
            throw new TipsException("拉取策略只能是 NEVER / ALWAYS / WHEN_MISSING", 400);
        }
        return upper;
    }
}
