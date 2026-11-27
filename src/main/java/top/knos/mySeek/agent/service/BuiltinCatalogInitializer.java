package top.knos.mySeek.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import top.knos.mySeek.agent.entity.AgentDefinition;
import top.knos.mySeek.agent.entity.ConnectorDefinition;
import top.knos.mySeek.agent.entity.SkillDefinition;
import top.knos.mySeek.agent.repository.AgentDefinitionRepository;
import top.knos.mySeek.agent.repository.ConnectorDefinitionRepository;
import top.knos.mySeek.agent.repository.SkillDefinitionRepository;

import java.util.UUID;

/** 首次启动写入内置目录；历史库缺表时跳过，绝不阻断应用启动。 */
@Slf4j
@Component
public class BuiltinCatalogInitializer {

    private final AgentDefinitionRepository agents;
    private final SkillDefinitionRepository skills;
    private final ConnectorDefinitionRepository connectors;

    public BuiltinCatalogInitializer(AgentDefinitionRepository agents,
                                     SkillDefinitionRepository skills,
                                     ConnectorDefinitionRepository connectors) {
        this.agents = agents;
        this.skills = skills;
        this.connectors = connectors;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        try {
            int added = 0;
            for (BuiltinCatalog.BuiltinAgent a : BuiltinCatalog.agents()) {
                if (agents.findByCodeAndCreatedBy(a.code(), BuiltinCatalog.SYSTEM_USER).isEmpty()) {
                    AgentDefinition entity = BuiltinCatalog.toEntity(a);
                    entity.setId(UUID.randomUUID().toString());
                    entity.setBuiltin(true);
                    entity.setEnabled(true);
                    entity.setCreatedBy(BuiltinCatalog.SYSTEM_USER);
                    agents.save(entity);
                    added++;
                }
            }
            for (BuiltinCatalog.BuiltinSkill s : BuiltinCatalog.skills()) {
                if (skills.findByCodeAndCreatedBy(s.code(), BuiltinCatalog.SYSTEM_USER).isEmpty()) {
                    SkillDefinition entity = BuiltinCatalog.toEntity(s);
                    entity.setId(UUID.randomUUID().toString());
                    entity.setBuiltin(true);
                    entity.setEnabled(true);
                    entity.setCreatedBy(BuiltinCatalog.SYSTEM_USER);
                    skills.save(entity);
                    added++;
                }
            }
            for (BuiltinCatalog.BuiltinConnector c : BuiltinCatalog.connectors()) {
                if (connectors.findByCodeAndCreatedBy(c.code(), BuiltinCatalog.SYSTEM_USER).isEmpty()) {
                    ConnectorDefinition entity = BuiltinCatalog.toEntity(c);
                    entity.setId(UUID.randomUUID().toString());
                    entity.setBuiltin(true);
                    entity.setEnabled(true);
                    entity.setCreatedBy(BuiltinCatalog.SYSTEM_USER);
                    connectors.save(entity);
                    added++;
                }
            }
            log.info("内置专家/技能/连接器初始化完成，新增 {} 项", added);
        } catch (Exception e) {
            log.warn("内置目录初始化跳过（请确认 V12 迁移已执行）: {}", e.getMessage());
        }
    }
}
