package top.knos.mySeek.agent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.knos.mySeek.agent.entity.AgentDefinition;
import top.knos.mySeek.agent.entity.SkillDefinition;
import top.knos.mySeek.agent.repository.AgentDefinitionRepository;
import top.knos.mySeek.agent.repository.ConnectorDefinitionRepository;
import top.knos.mySeek.agent.repository.SkillDefinitionRepository;
import top.knos.mySeek.agent.vo.CatalogVos;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CatalogServiceTest {

    private AgentDefinitionRepository agents;
    private SkillDefinitionRepository skills;
    private ConnectorDefinitionRepository connectors;
    private CatalogService service;

    @BeforeEach
    void setUp() {
        agents = mock(AgentDefinitionRepository.class);
        skills = mock(SkillDefinitionRepository.class);
        connectors = mock(ConnectorDefinitionRepository.class);
        service = new CatalogService(agents, skills, connectors);
        when(agents.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(skills.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void listsMergeCustomAndBuiltin() {
        AgentDefinition custom = agent("mine", 1L, false);
        AgentDefinition builtin = agent("general", BuiltinCatalog.SYSTEM_USER, true);
        when(agents.findByCreatedByOrderByBuiltinDescCreatedAtAsc(1L)).thenReturn(List.of(custom));
        when(agents.findByCreatedByOrderByBuiltinDescCreatedAtAsc(BuiltinCatalog.SYSTEM_USER)).thenReturn(List.of(builtin));

        List<CatalogVos.AgentVo> result = service.listAgents(1L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(v -> v.getCode().equals("mine")));
        assertTrue(result.stream().anyMatch(v -> Boolean.TRUE.equals(v.getBuiltin())));
    }

    @Test
    void saveAgentCreatesCustomAndRejectsBadCode() {
        CatalogVos.AgentSaveReq req = new CatalogVos.AgentSaveReq();
        req.setCode("my_expert");
        req.setName("我的专家");
        req.setSystemPrompt("人设");
        when(agents.findByCodeAndCreatedBy("my_expert", 1L)).thenReturn(Optional.empty());

        CatalogVos.AgentVo vo = service.saveAgent(1L, req);

        assertEquals("my_expert", vo.getCode());
        assertFalse(vo.getBuiltin());
        verify(agents).save(argThat(a -> a.getCreatedBy().equals(1L) && !a.getBuiltin()));

        req.setCode("bad code!");
        assertThrows(IllegalArgumentException.class, () -> service.saveAgent(1L, req));
    }

    @Test
    void findUsableAgentPrefersEnabledCustomOverBuiltin() {
        AgentDefinition builtin = agent("general", BuiltinCatalog.SYSTEM_USER, true);
        builtin.setSystemPrompt("内置人设");
        AgentDefinition custom = agent("general", 1L, false);
        custom.setSystemPrompt("自定义人设");
        when(agents.findByCodeAndCreatedBy("general", 1L)).thenReturn(Optional.of(custom));
        when(agents.findByCodeAndCreatedBy("general", BuiltinCatalog.SYSTEM_USER)).thenReturn(Optional.of(builtin));

        assertEquals("自定义人设", service.findUsableAgent(1L, "GENERAL").getSystemPrompt());
        custom.setEnabled(false);
        assertEquals("内置人设", service.findUsableAgent(1L, "general").getSystemPrompt());
    }

    @Test
    void builtinCannotBeDeletedAndForeignIsRejected() {
        AgentDefinition builtin = agent("general", BuiltinCatalog.SYSTEM_USER, true);
        when(agents.findById("a1")).thenReturn(Optional.of(builtin));
        assertThrows(IllegalArgumentException.class, () -> service.deleteAgent(1L, "a1"));

        AgentDefinition foreign = agent("mine", 2L, false);
        when(agents.findById("a2")).thenReturn(Optional.of(foreign));
        assertThrows(IllegalArgumentException.class, () -> service.deleteAgent(1L, "a2"));
        verify(agents, never()).delete(any());
    }

    @Test
    void findUsableSkillsSkipsDisabled() {
        SkillDefinition enabled = new SkillDefinition();
        enabled.setCode("web_research");
        enabled.setEnabled(true);
        SkillDefinition disabled = new SkillDefinition();
        disabled.setCode("draft_writing");
        disabled.setEnabled(false);
        when(skills.findByCodeAndCreatedBy("web_research", 1L)).thenReturn(Optional.of(enabled));
        when(skills.findByCodeAndCreatedBy("draft_writing", 1L)).thenReturn(Optional.of(disabled));

        List<SkillDefinition> result = service.findUsableSkills(1L, List.of("web_research", "draft_writing"));

        assertEquals(1, result.size());
        assertEquals("web_research", result.get(0).getCode());
    }

    private AgentDefinition agent(String code, Long owner, boolean builtin) {
        AgentDefinition e = new AgentDefinition();
        e.setId("id-" + code);
        e.setCode(code);
        e.setName(code);
        e.setCreatedBy(owner);
        e.setBuiltin(builtin);
        e.setEnabled(true);
        return e;
    }
}
