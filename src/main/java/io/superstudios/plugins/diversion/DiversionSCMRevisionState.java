package io.superstudios.plugins.diversion;

import hudson.scm.SCMRevisionState;

/**
 * Tracks the state of a Diversion repository at a specific point in time.
 * Used by Jenkins to determine what has changed between builds.
 */
public class DiversionSCMRevisionState extends SCMRevisionState {
    
    private final String commitId;
    private final long timestamp;
    
    public DiversionSCMRevisionState(String commitId, long timestamp) {
        this.commitId = commitId;
        this.timestamp = timestamp;
    }
    
    public String getCommitId() {
        return commitId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DiversionSCMRevisionState that = (DiversionSCMRevisionState) o;
        return timestamp == that.timestamp && 
               commitId.equals(that.commitId);
    }
    
    @Override
    public int hashCode() {
        return commitId.hashCode() + (int) timestamp;
    }
    
    @Override
    public String toString() {
        return "DiversionSCMRevisionState{" +
                "commitId='" + commitId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
