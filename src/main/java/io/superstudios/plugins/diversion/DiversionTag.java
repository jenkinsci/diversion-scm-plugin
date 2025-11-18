package io.superstudios.plugins.diversion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a tag in a Diversion repository.
 */
public class DiversionTag {
    private String id;
    private String name;
    private String commitId;
    private String description;
    private DiversionAuthor author;
    private long time;
    
    public DiversionTag(JsonNode node) {
        this.id = node.has("id") ? node.get("id").asText() : null;
        this.name = node.has("name") ? node.get("name").asText() : null;
        this.commitId = node.has("commit_id") ? node.get("commit_id").asText() : null;
        this.description = node.has("description") ? node.get("description").asText() : null;
        this.time = node.has("time") ? node.get("time").asLong() : 0;
        
        if (node.has("author") && !node.get("author").isNull()) {
            this.author = new DiversionAuthor(node.get("author"));
        }
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCommitId() {
        return commitId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public DiversionAuthor getAuthor() {
        return author;
    }
    
    public long getTime() {
        return time;
    }
    
    @Override
    public String toString() {
        return "DiversionTag{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", commitId='" + commitId + '\'' +
                ", description='" + description + '\'' +
                ", author=" + author +
                ", time=" + time +
                '}';
    }
}
