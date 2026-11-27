package top.knos.mySeek.agent.vo;

import lombok.Data;
import top.knos.mySeek.agent.entity.AgentDefinition;
import top.knos.mySeek.agent.entity.ConnectorDefinition;
import top.knos.mySeek.agent.entity.SkillDefinition;

import java.time.LocalDateTime;

/** 目录 VO 集合，集中定义以减少文件数量。 */
public final class CatalogVos {

    private CatalogVos() {}

    @Data
    public static class AgentVo {
        private String id;
        private String code;
        private String name;
        private String description;
        private String icon;
        private String systemPrompt;
        private String skillsJson;
        private String modelCode;
        private Boolean builtin;
        private Boolean enabled;
        private LocalDateTime createdAt;

        public static AgentVo from(AgentDefinition e) {
            AgentVo vo = new AgentVo();
            vo.setId(e.getId());
            vo.setCode(e.getCode());
            vo.setName(e.getName());
            vo.setDescription(e.getDescription());
            vo.setIcon(e.getIcon());
            vo.setSystemPrompt(e.getSystemPrompt());
            vo.setSkillsJson(e.getSkillsJson());
            vo.setModelCode(e.getModelCode());
            vo.setBuiltin(e.getBuiltin());
            vo.setEnabled(e.getEnabled());
            vo.setCreatedAt(e.getCreatedAt());
            return vo;
        }
    }

    @Data
    public static class SkillVo {
        private String id;
        private String code;
        private String name;
        private String description;
        private String icon;
        private String promptTemplate;
        private String toolsJson;
        private Boolean builtin;
        private Boolean enabled;
        private LocalDateTime createdAt;

        public static SkillVo from(SkillDefinition e) {
            SkillVo vo = new SkillVo();
            vo.setId(e.getId());
            vo.setCode(e.getCode());
            vo.setName(e.getName());
            vo.setDescription(e.getDescription());
            vo.setIcon(e.getIcon());
            vo.setPromptTemplate(e.getPromptTemplate());
            vo.setToolsJson(e.getToolsJson());
            vo.setBuiltin(e.getBuiltin());
            vo.setEnabled(e.getEnabled());
            vo.setCreatedAt(e.getCreatedAt());
            return vo;
        }
    }

    @Data
    public static class ConnectorVo {
        private String id;
        private String code;
        private String name;
        private String description;
        private String icon;
        private ConnectorDefinition.Type type;
        private String toolsJson;
        private Boolean builtin;
        private Boolean enabled;
        private LocalDateTime createdAt;

        public static ConnectorVo from(ConnectorDefinition e) {
            ConnectorVo vo = new ConnectorVo();
            vo.setId(e.getId());
            vo.setCode(e.getCode());
            vo.setName(e.getName());
            vo.setDescription(e.getDescription());
            vo.setIcon(e.getIcon());
            vo.setType(e.getType());
            vo.setToolsJson(e.getToolsJson());
            vo.setBuiltin(e.getBuiltin());
            vo.setEnabled(e.getEnabled());
            vo.setCreatedAt(e.getCreatedAt());
            return vo;
        }
    }

    @Data
    public static class AgentSaveReq {
        private String code;
        private String name;
        private String description;
        private String icon;
        private String systemPrompt;
        private String skillsJson;
        private String modelCode;
        private Boolean enabled;
    }

    @Data
    public static class SkillSaveReq {
        private String code;
        private String name;
        private String description;
        private String icon;
        private String promptTemplate;
        private String toolsJson;
        private Boolean enabled;
    }

    @Data
    public static class ConnectorSaveReq {
        private String code;
        private String name;
        private String description;
        private String icon;
        private ConnectorDefinition.Type type;
        private String toolsJson;
        private Boolean enabled;
    }
}
