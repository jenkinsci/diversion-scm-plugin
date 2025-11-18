package io.superstudios.plugins.diversion;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMHead;

/**
 * Represents a Diversion branch as an SCMHead.
 * SCMHead is Jenkins' abstraction for branches, tags, pull requests, etc.
 */
public class DiversionSCMHead extends SCMHead {
    
    private final String branchId; // e.g., "dv.branch.8"
    
    /**
     * Constructor
     * @param branchName Human-readable branch name (e.g., "dev", "main")
     * @param branchId Diversion branch ID (e.g., "dv.branch.8")
     */
    public DiversionSCMHead(@NonNull String branchName, @NonNull String branchId) {
        super(branchName);
        this.branchId = branchId;
    }
    
    /**
     * Get the Diversion branch ID
     */
    public String getBranchId() {
        return branchId;
    }
    
    @Override
    public String toString() {
        return "DiversionSCMHead{" +
                "name='" + getName() + '\'' +
                ", branchId='" + branchId + '\'' +
                '}';
    }
}

