package com.huaweicloud.sermant.core.plugin.config;


/**
 * @author jeff.yang
 * @date 2023-04-25 17:00:30
 **/
public class PluginMeta {
    private String name;
    private String version;
    private String type;
    private String agentArgs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAgentArgs() {
        return agentArgs;
    }

    public void setAgentArgs(String agentArgs) {
        this.agentArgs = agentArgs;
    }
}
