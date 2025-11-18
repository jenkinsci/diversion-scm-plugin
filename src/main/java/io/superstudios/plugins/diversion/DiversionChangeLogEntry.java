package io.superstudios.plugins.diversion;

import hudson.scm.ChangeLogSet;
import hudson.model.User;

import java.util.Date;

/**
 * Represents a single commit in the Diversion change log.
 * This is what users see in the "Changes" section of Jenkins builds.
 */
public class DiversionChangeLogEntry extends ChangeLogSet.Entry {
    
    private final String commitId;
    private final String message;
    private final DiversionAuthor author;
    private final long timestamp;
    private final java.util.Collection<String> affectedPaths;
    
    public DiversionChangeLogEntry(String commitId, String message, DiversionAuthor author, long timestamp) {
        this.commitId = commitId;
        this.message = message;
        this.author = author;
        this.timestamp = timestamp;
        this.affectedPaths = java.util.Collections.emptyList();
    }
    
    public DiversionChangeLogEntry(String commitId, String message, DiversionAuthor author, long timestamp, java.util.Collection<String> affectedPaths) {
        this.commitId = commitId;
        this.message = message;
        this.author = author;
        this.timestamp = timestamp;
        this.affectedPaths = affectedPaths != null ? affectedPaths : java.util.Collections.emptyList();
    }
    
    @Override
    public String getMsg() {
        return message;
    }
    
    @Override
    public User getAuthor() {
        if (author != null && author.getName() != null) {
            return User.get(author.getName(), true);
        }
        return User.getUnknown();
    }
    
    @Override
    public String getCommitId() {
        return commitId;
    }
    
    public DiversionAuthor getDiversionAuthor() {
        return author;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Date getDate() {
        return new Date(timestamp * 1000);
    }
    
    @Override
    public String toString() {
        return commitId + ": " + message;
    }
    
    /**
     * Get a display string that includes commit ID.
     * This might be used by Jenkins core templates.
     */
    public String getDisplayName() {
        if (commitId != null && !commitId.isEmpty()) {
            return commitId + " — " + message;
        }
        return message;
    }
    
    /**
     * Get formatted message with commit ID prefix.
     * Some Jenkins templates might use this.
     */
    public String getMsgAnnotated() {
        if (commitId != null && !commitId.isEmpty()) {
            return commitId + " — " + message;
        }
        return message;
    }
    
    @Override
    public java.util.Collection<String> getAffectedPaths() {
        return affectedPaths;
    }
    
    /**
     * Package-private method to set the parent ChangeLogSet.
     * Called from DiversionChangeLogParser after creating the set.
     */
    void setParentSet(ChangeLogSet<? extends ChangeLogSet.Entry> parent) {
        setParent(parent);
    }
}
