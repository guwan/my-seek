package top.knos.mySeek.agent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.knos.mySeek.agent.entity.AgentDefinition;
import top.knos.mySeek.agent.entity.ConnectorDefinition;
import top.knos.mySeek.agent.entity.SkillDefinition;
import top.knos.mySeek.agent.repository.AgentDefinitionRepository;
import top.knos.mySeek.agent.repository.ConnectorDefinitionRepository;
import top.knos.mySeek.agent.repository.SkillDefinitionRepository;
import top.knos.mySeek.agent.vo.CatalogVos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 目录服务：内置项对所有用户可见、只读；自定义项按用户隔离、可 CRUD。 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final AgentDefinitionRepository agents;
    private final SkillDefinitionRepository skills;
    private final ConnectorDefinitionRepository connectors;

    public List<CatalogVos.AgentVo> listAgents(Long userId) {
        return merge(agents.findByCreatedByOrderByBuiltinDescCreatedAtAsc(userId),
                agents.findByCreatedByOrderByBuiltinDescCreatedAtAsc(BuiltinCatalog.SYSTEM_USER),
                CatalogVos.AgentVo::from);
    }

    public List<CatalogVos.SkillVo> listSkills(Long userId) {
        return merge(skills.findByCreatedByOrderByBuiltinDescCreatedAtAsc(userId),
                skills.findByCreatedByOrderByBuiltinDescCreatedAtAsc(BuiltinCatalog.SYSTEM_USER),
                CatalogVos.SkillVo::from);
    }

    public List<CatalogVos.ConnectorVo> listConnectors(Long userId) {
        return merge(connectors.findByCreatedByOrderByBuiltinDescCreatedAtAsc(userId),
                connectors.findByCreatedByOrderByBuiltinDescCreatedAtAsc(BuiltinCatalog.SYSTEM_USER),
                CatalogVos.ConnectorVo::from);
    }

    public CatalogVos.AgentVo saveAgent(Long userId, CatalogVos.AgentSaveReq req) {
        require(req != null && validCode(req.getCode()), "code 只能包含字母、数字、下划线和连字符");
        require(req.getName() != null && !req.getName().isBlank(), "名称不能为空");
        AgentDefinition e = agents.findByCodeAndCreatedBy(req.getCode().trim(), userId).orElseGet(AgentDefinition::new);
        boolean created = e.getId() == null;
        if (created) {
            e.setId(UUID.randomUUID().toString());
            e.setCode(req.getCode().trim().toLowerCase(Locale.ROOT));
            e.setCreatedBy(userId);
            e.setBuiltin(false);
        }
        e.setName(req.getName().trim());
        e.setDescription(req.getDescription());
        e.setIcon(req.getIcon());
        e.setSystemPrompt(req.getSystemPrompt());
        e.setSkillsJson(req.getSkillsJson());
        e.setModelCode(req.getModelCode());
        e.setEnabled(req.getEnabled() == null || req.getEnabled());
        return CatalogVos.AgentVo.from(agents.save(e));
    }

    public CatalogVos.SkillVo saveSkill(Long userId, CatalogVos.SkillSaveReq req) {
        require(req != null && validCode(req.getCode()), "code 只能包含字母、数字、下划线和连字符");
        require(req.getName() != null && !req.getName().isBlank(), "名称不能为空");
        SkillDefinition e = skills.findByCodeAndCreatedBy(req.getCode().trim(), userId).orElseGet(SkillDefinition::new);
        boolean created = e.getId() == null;
        if (created) {
            e.setId(UUID.randomUUID().toString());
            e.setCode(req.getCode().trim().toLowerCase(Locale.ROOT));
            e.setCreatedBy(userId);
            e.setBuiltin(false);
        }
        e.setName(req.getName().trim());
        e.setDescription(req.getDescription());
        e.setIcon(req.getIcon());
        e.setPromptTemplate(req.getPromptTemplate());
        e.setToolsJson(req.getToolsJson());
        e.setEnabled(req.getEnabled() == null || req.getEnabled());
        return CatalogVos.SkillVo.from(skills.save(e));
    }

    public CatalogVos.ConnectorVo saveConnector(Long userId, CatalogVos.ConnectorSaveReq req) {
        require(req != null && validCode(req.getCode()), "code 只能包含字母、数字、下划线和连字符");
        require(req.getName() != null && !req.getName().isBlank(), "名称不能为空");
        ConnectorDefinition e = connectors.findByCodeAndCreatedBy(req.getCode().trim(), userId).orElseGet(ConnectorDefinition::new);
        boolean created = e.getId() == null;
        if (created) {
            e.setId(UUID.randomUUID().toString());
            e.setCode(req.getCode().trim().toLowerCase(Locale.ROOT));
            e.setCreatedBy(userId);
            e.setBuiltin(false);
        }
        e.setName(req.getName().trim());
        e.setDescription(req.getDescription());
        e.setIcon(req.getIcon());
        e.setType(req.getType() == null ? ConnectorDefinition.Type.BUILTIN : req.getType());
        e.setToolsJson(req.getToolsJson());
        e.setEnabled(req.getEnabled() == null || req.getEnabled());
        return CatalogVos.ConnectorVo.from(connectors.save(e));
    }

    /** 删除自定义项；内置项不可删除，只能停用。 */
    public void deleteAgent(Long userId, String id) {
        delete(agents, userId, id, "专家");
    }

    public void deleteSkill(Long userId, String id) {
        delete(skills, userId, id, "技能");
    }

    public void deleteConnector(Long userId, String id) {
        delete(connectors, userId, id, "连接器");
    }

    private <T> void delete(org.springframework.data.jpa.repository.JpaRepository<T, String> repo,
                            Long userId, String id, String label) {
        T entity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException(label + "不存在"));
        Long owner = ownerOf(entity);
        require(owner == null || owner != BuiltinCatalog.SYSTEM_USER, "内置" + label + "不可删除，可停用");
        require(userId.equals(owner), "无权删除他人的" + label);
        repo.delete(entity);
    }

    /** 供任务中心解析专家人设与技能指令。内置项或本人自定义项可用。 */
    public AgentDefinition findUsableAgent(Long userId, String code) {
        if (code == null || code.isBlank()) return null;
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        AgentDefinition custom = agents.findByCodeAndCreatedBy(normalized, userId).orElse(null);
        if (custom != null && Boolean.TRUE.equals(custom.getEnabled())) return custom;
        AgentDefinition builtin = agents.findByCodeAndCreatedBy(normalized, BuiltinCatalog.SYSTEM_USER).orElse(null);
        return builtin != null && Boolean.TRUE.equals(builtin.getEnabled()) ? builtin : null;
    }

    public List<SkillDefinition> findUsableSkills(Long userId, List<String> codes) {
        if (codes == null || codes.isEmpty()) return List.of();
        List<SkillDefinition> result = new ArrayList<>();
        for (String code : codes) {
            String normalized = code.trim().toLowerCase(Locale.ROOT);
            SkillDefinition e = skills.findByCodeAndCreatedBy(normalized, userId)
                    .orElseGet(() -> skills.findByCodeAndCreatedBy(normalized, BuiltinCatalog.SYSTEM_USER).orElse(null));
            if (e != null && Boolean.TRUE.equals(e.getEnabled())) result.add(e);
        }
        return result;
    }

    private static Long ownerOf(Object entity) {
        if (entity instanceof AgentDefinition a) return a.getCreatedBy();
        if (entity instanceof SkillDefinition s) return s.getCreatedBy();
        if (entity instanceof ConnectorDefinition c) return c.getCreatedBy();
        return null;
    }

    private static <T, V> List<V> merge(List<T> custom, List<T> builtin, java.util.function.Function<T, V> mapper) {
        List<V> result = new ArrayList<>();
        if (custom != null) custom.forEach(e -> result.add(mapper.apply(e)));
        if (builtin != null) builtin.forEach(e -> result.add(mapper.apply(e)));
        return result;
    }

    private static boolean validCode(String code) {
        return code != null && code.matches("^[a-zA-Z0-9_-]{2,64}$");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
