package io.superstudios.plugins.diversion;

import hudson.model.Run;
import hudson.scm.ChangeLogSet;

import java.util.List;
import java.util.Iterator;

/**
 * Represents a set of changes from Diversion commits.
 * This is what Jenkins displays in the "Changes" section.
 */
public class DiversionChangeLogSet extends ChangeLogSet<DiversionChangeLogEntry> {
    
    private final List<DiversionChangeLogEntry> entries;
    
    public DiversionChangeLogSet(Run<?, ?> run, List<DiversionChangeLogEntry> entries) {
        super(run, null);
        this.entries = entries;
        // Note: Parent is set in DiversionChangeLogParser after the set is created
        // to avoid issues with protected setParent() method
    }
    
    @Override
    public boolean isEmptySet() {
        return entries == null || entries.isEmpty();
    }
    
    @Override
    public String getKind() {
        return "Diversion";
    }
    
    @Override
    public Iterator<DiversionChangeLogEntry> iterator() {
        return entries.iterator();
    }
}
