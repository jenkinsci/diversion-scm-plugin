package io.superstudios.plugins.diversion;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Diversion commit
 */
public class DiversionCommit {
    private String commitId;
    private long createdTs;
    private String commitMessage;
    private String branchId;
    private DiversionAuthor author;
    private String[] parents;
    private List<String> changedFiles;
    
    public DiversionCommit() {}
    
    public DiversionCommit(JsonNode json) {
        this.commitId = json.get("commit_id").asText();
        this.createdTs = json.get("created_ts").asLong();
        this.commitMessage = json.get("commit_message").asText();
        this.branchId = json.get("branch_id").asText();
        
        JsonNode authorNode = json.get("author");
        if (authorNode != null) {
            this.author = new DiversionAuthor(authorNode);
        }
        
        JsonNode parentsNode = json.get("parents");
        if (parentsNode != null && parentsNode.isArray()) {
            this.parents = new String[parentsNode.size()];
            for (int i = 0; i < parentsNode.size(); i++) {
                this.parents[i] = parentsNode.get(i).asText();
            }
        }
        
        // Parse changed files if available
        this.changedFiles = new ArrayList<>();
        JsonNode filesNode = json.get("files");
        if (filesNode != null && filesNode.isArray()) {
            for (JsonNode fileNode : filesNode) {
                String filePath = fileNode.asText();
                this.changedFiles.add(filePath);
            }
        }
    }
    
    // Getters and setters
    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }
    
    public long getCreatedTs() { return createdTs; }
    public void setCreatedTs(long createdTs) { this.createdTs = createdTs; }
    
    public String getCommitMessage() { return commitMessage; }
    public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }
    
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    
    public DiversionAuthor getAuthor() { return author; }
    public void setAuthor(DiversionAuthor author) { this.author = author; }
    
    public String[] getParents() { return parents; }
    public void setParents(String[] parents) { this.parents = parents; }
    
    public List<String> getChangedFiles() { return changedFiles; }
    public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }
}
