package io.superstudios.plugins.diversion;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;

/**
 * Represents a specific commit/revision in Diversion.
 * Used by Jenkins to track changes and determine when to rebuild.
 */
public class DiversionSCMRevision extends SCMRevision {
    
    private final String commitId; // e.g., "dv.commit.4762"
    
    /**
     * Constructor
     * @param head The SCMHead (branch) this revision belongs to
     * @param commitId The Diversion commit ID
     */
    public DiversionSCMRevision(@NonNull SCMHead head, @NonNull String commitId) {
        super(head);
        this.commitId = commitId;
    }
    
    /**
     * Get the Diversion commit ID
     */
    public String getCommitId() {
        return commitId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DiversionSCMRevision that = (DiversionSCMRevision) o;
        return commitId.equals(that.commitId);
    }
    
    @Override
    public int hashCode() {
        return commitId.hashCode();
    }
    
    @Override
    public String toString() {
        return "DiversionSCMRevision{" +
                "commitId='" + commitId + '\'' +
                ", head=" + getHead() +
                '}';
    }
}

