package io.superstudios.plugins.diversion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Diversion repository
 */
public class DiversionRepository {
    
    private final String id;
    private final String name;
    private final String description;
    private final String defaultBranch;
    
    public DiversionRepository(JsonNode json) {
        this.id = json.get("repo_id").asText();
        this.name = json.get("repo_name").asText();
        this.description = json.has("description") ? json.get("description").asText() : "";
        this.defaultBranch = json.has("default_branch_id") ? json.get("default_branch_id").asText() : "main";
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getDefaultBranch() {
        return defaultBranch;
    }
    
    @Override
    public String toString() {
        return "DiversionRepository{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", defaultBranch='" + defaultBranch + '\'' +
                '}';
    }
}
