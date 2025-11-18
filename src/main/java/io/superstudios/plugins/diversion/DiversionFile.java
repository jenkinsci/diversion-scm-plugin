package io.superstudios.plugins.diversion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a file in a Diversion repository
 */
public class DiversionFile {
    
    private String path;
    private String type;
    private String blobId;
    private long size;
    
    public DiversionFile() {
        // Default constructor
    }
    
    public DiversionFile(JsonNode json) {
        this.path = json.get("path").asText();
        this.type = json.get("type").asText();
        this.blobId = json.has("blobId") ? json.get("blobId").asText() : null;
        this.size = json.has("size") ? json.get("size").asLong() : 0;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public void setBlobId(String blobId) {
        this.blobId = blobId;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getType() {
        return type;
    }
    
    public String getBlobId() {
        return blobId;
    }
    
    public long getSize() {
        return size;
    }
    
    public boolean isFile() {
        return "blob".equals(type);
    }
    
    public boolean isDirectory() {
        return "tree".equals(type);
    }
    
    @Override
    public String toString() {
        return "DiversionFile{" +
                "path='" + path + '\'' +
                ", type='" + type + '\'' +
                ", blobId='" + blobId + '\'' +
                ", size=" + size +
                '}';
    }
}
