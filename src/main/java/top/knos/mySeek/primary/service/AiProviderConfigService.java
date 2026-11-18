package top.knos.mySeek.primary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.primary.model.OpenAICompatibleProvider;
import top.knos.mySeek.primary.model.entity.AiProviderConfig;
import top.knos.mySeek.primary.model.repository.AiProviderConfigRepository;
import top.knos.mySeek.primary.vo.AiProviderRequest;
import top.knos.mySeek.primary.vo.AiProviderVo;
import top.knos.mySeek.security.support.EncryptUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 用户自定义 AI 接口配置服务：持久化配置，并在运行时注册/注销到 {@link ModelProviderManager}。
 */
@Service
@Slf4j
public class AiProviderConfigService {

    private final AiProviderConfigRepository repository;
    private final ModelProviderManager providerManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 用于加密 API Key 的口令，生产环境务必通过环境变量覆盖 */
    @Value("${ai-provider.secret:myseek-local-dev-secret-change-me}")
    private String encryptSecret;

    public AiProviderConfigService(AiProviderConfigRepository repository, ModelProviderManager providerManager) {
        this.repository = repository;
        this.providerManager = providerManager;
    }

    /** 应用启动后把已保存的启用配置注册进管理器 */
    @PostConstruct
    public void loadAndRegister() {
        try {
            for (AiProviderConfig config : repository.findAll()) {
                if (config.isEnabled()) {
                    register(config);
                }
            }
            log.info("已加载用户自定义 AI 接口配置");
        } catch (Exception e) {
            // 数据库不可用（如首次启动、表未建）时不应阻断应用
            log.warn("加载 AI 接口配置失败，将在首次访问时重试: {}", e.getMessage());
        }
    }

    /** 列出全部配置（含内置），api-key 脱敏 */
    public List<AiProviderVo> list() {
        List<AiProviderVo> result = new ArrayList<>();

        // 内置提供者
        providerManager.getAllProviders().entrySet().stream()
                .filter(e -> isBuiltIn(e.getKey()))
                .forEach(e -> {
                    AiProviderVo vo = new AiProviderVo();
                    vo.setName(e.getKey());
                    vo.setDisplayName(displayNameFor(e.getKey()));
                    vo.setModels(new ArrayList<>(e.getValue().getAvailableModels()));
                    vo.setEnabled(e.getValue().isAvailable());
                    vo.setBuiltIn(true);
                    vo.setApiKeyConfigured(false);
                    result.add(vo);
                });

        // 用户自定义
        repository.findAllByOrderByCreatedAtDesc().forEach(c -> result.add(toVo(c)));
        return result;
    }

    /** 新增配置 */
    @Transactional
    public AiProviderVo create(AiProviderRequest req) {
        validate(req, true);
        AiProviderConfig config = new AiProviderConfig();
        applyRequest(config, req, true);
        config = repository.save(config);
        if (config.isEnabled()) {
            register(config);
        }
        return toVo(config);
    }

    /** 更新配置 */
    @Transactional
    public AiProviderVo update(String id, AiProviderRequest req) {
        AiProviderConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("配置不存在: " + id));
        // 名称变化时先注销旧名称
        if (!config.getName().equals(req.getName())) {
            providerManager.unregisterProvider(config.getName());
        }
        validate(req, false);
        applyRequest(config, req, false);
        config = repository.save(config);
        providerManager.unregisterProvider(config.getName());
        if (config.isEnabled()) {
            register(config);
        }
        return toVo(config);
    }

    /** 删除配置 */
    @Transactional
    public void delete(String id) {
        AiProviderConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("配置不存在: " + id));
        providerManager.unregisterProvider(config.getName());
        repository.delete(config);
    }

    /** 切换启用状态 */
    @Transactional
    public AiProviderVo toggle(String id, boolean enabled) {
        AiProviderConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("配置不存在: " + id));
        config.setEnabled(enabled);
        config = repository.save(config);
        if (enabled) register(config);
        else providerManager.unregisterProvider(config.getName());
        return toVo(config);
    }

    /** 测试接口连通性：用配置里的第一个模型发一条极简消息 */
    public void test(AiProviderRequest req) {
        String key = req.getApiKey();
        if ((key == null || key.isBlank()) && req.getId() != null) {
            AiProviderConfig existing = repository.findById(req.getId())
                    .orElseThrow(() -> new IllegalArgumentException("配置不存在: " + req.getId()));
            key = decrypt(existing.getApiKey());
        }
        List<String> models = (req.getModels() == null || req.getModels().isEmpty())
                ? List.of("test") : req.getModels();
        OpenAICompatibleProvider provider = new OpenAICompatibleProvider(
                req.getName() + "-test", key, req.getBaseUrl(), models);
        if (!provider.isAvailable()) {
            throw new IllegalArgumentException("配置不完整：请填写 base-url、api-key 和至少一个模型");
        }
        provider.createChatClient(models.get(0))
                .prompt("请只回复 OK")
                .call()
                .content();
    }

    private void applyRequest(AiProviderConfig config, AiProviderRequest req, boolean create) {
        config.setName(req.getName());
        config.setDisplayName(req.getDisplayName());
        config.setBaseUrl(normalizeBaseUrl(req.getBaseUrl()));
        if (create || (req.getApiKey() != null && !req.getApiKey().isBlank())) {
            config.setApiKey(encrypt(req.getApiKey()));
        }
        config.setModelsJson(toJson(req.getModels()));
        config.setEnabled(req.isEnabled());
    }

    private void validate(AiProviderRequest req, boolean create) {
        if (isBuiltIn(req.getName())) {
            throw new IllegalArgumentException("接口标识 " + req.getName() + " 与内置提供者冲突");
        }
        Optional<AiProviderConfig> existing = repository.findByName(req.getName());
        if (existing.isPresent() && (create || !existing.get().getId().equals(req.getId()))) {
            throw new IllegalArgumentException("接口标识已存在: " + req.getName());
        }
        if (create && (req.getApiKey() == null || req.getApiKey().isBlank())) {
            throw new IllegalArgumentException("api-key 不能为空");
        }
    }

    private void register(AiProviderConfig config) {
        List<String> models = parseModels(config.getModelsJson());
        OpenAICompatibleProvider provider = new OpenAICompatibleProvider(
                config.getName(), decrypt(config.getApiKey()), config.getBaseUrl(), models);
        providerManager.registerProvider(config.getName(), provider);
    }

    private String normalizeBaseUrl(String url) {
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if (!u.endsWith("/v1") && !u.contains("/v1/")) {
            u = u + "/v1";
        }
        return u;
    }

    private String toJson(List<String> models) {
        try {
            return objectMapper.writeValueAsString(models == null ? List.of() : models);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> parseModels(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            String[] arr = objectMapper.readValue(json, String[].class);
            return Arrays.asList(arr);
        } catch (Exception e) {
            log.warn("解析模型列表失败: {}", json);
            return List.of();
        }
    }

    private String encrypt(String raw) {
        return raw == null ? "" : EncryptUtil.getInstance().AESencode(raw, encryptSecret);
    }

    private String decrypt(String cipher) {
        if (cipher == null || cipher.isBlank()) return "";
        try {
            return EncryptUtil.getInstance().AESdecode(cipher, encryptSecret);
        } catch (Exception e) {
            log.warn("API Key 解密失败，可能加密口令已变更");
            return "";
        }
    }

    private AiProviderVo toVo(AiProviderConfig c) {
        AiProviderVo vo = new AiProviderVo();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setDisplayName(c.getDisplayName());
        vo.setBaseUrl(c.getBaseUrl());
        List<String> models = parseModels(c.getModelsJson());
        vo.setModels(models);
        vo.setEnabled(c.isEnabled());
        vo.setBuiltIn(false);
        vo.setApiKeyConfigured(c.getApiKey() != null && !c.getApiKey().isBlank());
        vo.setApiKeyMasked(mask(decrypt(c.getApiKey())));
        vo.setCreatedAt(c.getCreatedAt() == null ? null : c.getCreatedAt().toString());
        vo.setUpdatedAt(c.getUpdatedAt() == null ? null : c.getUpdatedAt().toString());
        return vo;
    }

    private String mask(String key) {
        if (key == null || key.isBlank()) return "";
        if (key.length() <= 8) return key.substring(0, 1) + "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private boolean isBuiltIn(String name) {
        return "ollama".equals(name) || "doubao".equals(name) || "qwen".equals(name);
    }

    private String displayNameFor(String name) {
        return switch (name) {
            case "ollama" -> "Ollama 本地模型";
            case "doubao" -> "豆包（火山引擎）";
            case "qwen" -> "通义千问（阿里云）";
            default -> name;
        };
    }
}
