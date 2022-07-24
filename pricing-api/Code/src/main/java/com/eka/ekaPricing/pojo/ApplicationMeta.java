package com.eka.ekaPricing.pojo;

import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
"_id",
"title",
"name",
"tenantID",
"ObjectMetaIds",
"version",
"createdOn",
"createdBy",
"lastModifiedOn",
"lastModifiedBy"
})
@Document(collection = "Boliden_Application")
public class ApplicationMeta {

@JsonProperty("_id")
private String id;
@JsonProperty("title")
private String title;
@JsonProperty("name")
private String name;
@JsonProperty("tenantID")
private String tenantID;
@JsonProperty("ObjectMetaIds")
private List<String> ObjectMetaIds =null;
@JsonProperty("version")
private String version;
@JsonProperty("createdOn")
private String createdOn;
@JsonProperty("createdBy")
private String createdBy;
@JsonProperty("lastModifiedOn")
private String lastModifiedOn;
@JsonProperty("lastModifiedBy")
private String lastModifiedBy;

@JsonProperty("_id")
public String getId() {
return id;
}

@JsonProperty("_id")
public void setId(String id) {
this.id = id;
}

@JsonProperty("title")
public String getTitle() {
return title;
}

@JsonProperty("title")
public void setTitle(String title) {
this.title = title;
}

@JsonProperty("name")
public String getName() {
return name;
}

@JsonProperty("name")
public void setName(String name) {
this.name = name;
}

@JsonProperty("tenantID")
public String getTenantID() {
return tenantID;
}

@JsonProperty("tenantID")
public void setTenantID(String tenantID) {
this.tenantID = tenantID;
}

@JsonProperty("ObjectMetaIds")
public List<String> getObjectMetaIds() {	
return ObjectMetaIds;
}

@JsonProperty("ObjectMetaIds")
public void setObjectMetaIds(List<String> objectMetaIds) {
this.ObjectMetaIds = objectMetaIds;
}

@JsonProperty("version")
public String getVersion() {
return version;
}

@JsonProperty("version")
public void setVersion(String version) {
this.version = version;
}

@JsonProperty("createdOn")
public String getCreatedOn() {
return createdOn;
}

@JsonProperty("createdOn")
public void setCreatedOn(String createdOn) {
this.createdOn = createdOn;
}

@JsonProperty("createdBy")
public String getCreatedBy() {
return createdBy;
}

@JsonProperty("createdBy")
public void setCreatedBy(String createdBy) {
this.createdBy = createdBy;
}

@JsonProperty("lastModifiedOn")
public String getLastModifiedOn() {
return lastModifiedOn;
}

@JsonProperty("lastModifiedOn")
public void setLastModifiedOn(String lastModifiedOn) {
this.lastModifiedOn = lastModifiedOn;
}

@JsonProperty("lastModifiedBy")
public String getLastModifiedBy() {
return lastModifiedBy;
}

@JsonProperty("lastModifiedBy")
public void setLastModifiedBy(String lastModifiedBy) {
this.lastModifiedBy = lastModifiedBy;
}

}