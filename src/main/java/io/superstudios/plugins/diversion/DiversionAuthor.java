package io.superstudios.plugins.diversion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Diversion commit author
 */
public class DiversionAuthor {
    private String id;
    private String name;
    private String fullName;
    private String email;
    private String image;
    
    public DiversionAuthor() {}
    
    public DiversionAuthor(JsonNode json) {
        this.id = json.has("id") ? json.get("id").asText() : null;
        this.name = json.has("name") ? json.get("name").asText() : null;
        this.fullName = json.has("full_name") ? json.get("full_name").asText() : null;
        this.email = json.has("email") ? json.get("email").asText() : null;
        this.image = json.has("image") ? json.get("image").asText() : null;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
