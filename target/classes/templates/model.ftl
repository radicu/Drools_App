package ${packageName};

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ${className} {

<#list fields as field>
    private ${field.type} ${field.name?uncap_first};
</#list>

<#list fields as field>

    @JsonProperty("${field.name?uncap_first}")
    public ${field.type} get${field.name?cap_first}() {
        return ${field.name?uncap_first};
    }

    public void set${field.name?cap_first}(${field.type} ${field.name?uncap_first}) {
        this.${field.name?uncap_first} = ${field.name?uncap_first};
    }
</#list>
}
