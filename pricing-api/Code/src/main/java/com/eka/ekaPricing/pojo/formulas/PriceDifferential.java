
package com.eka.ekaPricing.pojo.formulas;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;




@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
	"differentialType",
	"diffUpperThreshold",
	"diffLowerThreashold",
	"differentialValue",
	"differentialUnit"
})
public class PriceDifferential {

    @JsonProperty("differentialType")
    private String differentialType;
    @JsonProperty("diffUpperThreshold")
	private String diffUpperThreshold;
    @JsonProperty("diffLowerThreashold")
	private String diffLowerThreashold;
    @JsonProperty("differentialValue")
	private String differentialValue;
    @JsonProperty("differentialUnit")
	private String differentialUnit;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("differentialType")
    public String getDifferentialType() {
		return differentialType;
	}

    @JsonProperty("differentialType")
	public void setDifferentialType(String differentialType) {
		this.differentialType = differentialType;
	}

    @JsonProperty("diffUpperThreshold")
	public String getDiffUpperThreshold() {
		return diffUpperThreshold;
	}

    @JsonProperty("diffUpperThreshold")
	public void setDiffUpperThreshold(String diffUpperThreshold) {
		this.diffUpperThreshold = diffUpperThreshold;
	}

    @JsonProperty("diffLowerThreashold")
	public String getDiffLowerThreashold() {
		return diffLowerThreashold;
	}

    @JsonProperty("diffLowerThreashold")
	public void setDiffLowerThreashold(String diffLowerThreashold) {
		this.diffLowerThreashold = diffLowerThreashold;
	}

    @JsonProperty("differentialValue")
	public String getDifferentialValue() {
		return differentialValue;
	}

    @JsonProperty("differentialValue")
	public void setDifferentialValue(String differentialValue) {
		this.differentialValue = differentialValue;
	}

    @JsonProperty("differentialUnit")
	public String getDifferentialUnit() {
		return differentialUnit;
	}

    @JsonProperty("differentialUnit")
	public void setDifferentialUnit(String differentialUnit) {
		this.differentialUnit = differentialUnit;
	}
    
	@JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }
	
	@JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
