package io.superstudios.plugins.diversion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Diversion branch
 */
public class DiversionBranch {
    private final String branchId;
    private final String branchName;
    private final String commitId;
    
    public DiversionBranch(String branchId, String branchName, String commitId) {
        this.branchId = branchId;
        this.branchName = branchName;
        this.commitId = commitId;
    }
    
    public DiversionBranch(JsonNode json) {
        this.branchId = json.has("branch_id") ? json.get("branch_id").asText() : 
                        json.has("id") ? json.get("id").asText() : null;
        this.branchName = json.has("branch_name") ? json.get("branch_name").asText() :
                         json.has("name") ? json.get("name").asText() : null;
        this.commitId = json.has("commit_id") ? json.get("commit_id").asText() :
                       json.has("commit") ? json.get("commit").asText() : null;
    }
    
    public String getBranchId() {
        return branchId;
    }
    
    public String getBranchName() {
        return branchName;
    }
    
    public String getCommitId() {
        return commitId;
    }
    
    // Convenience methods for consistency with other classes
    public String getId() {
        return branchId;
    }
    
    public String getName() {
        return branchName;
    }
    
    @Override
    public String toString() {
        return "DiversionBranch{" +
                "branchId='" + branchId + '\'' +
                ", branchName='" + branchName + '\'' +
                ", commitId='" + commitId + '\'' +
                '}';
    }
}

