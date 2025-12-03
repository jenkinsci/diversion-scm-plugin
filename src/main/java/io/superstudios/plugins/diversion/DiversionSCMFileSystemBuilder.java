package io.superstudios.plugins.diversion;

import hudson.Extension;
import hudson.model.Item;
import hudson.scm.SCM;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

/**
 * Builder for creating DiversionSCMFileSystem instances.
 * This tells Jenkins how to create a file system for Diversion SCM sources.
 */
@Extension
public class DiversionSCMFileSystemBuilder extends SCMFileSystem.Builder {
    
    @Override
    public boolean supports(SCM source) {
        return source instanceof DiversionSCM;
    }
    
    @Override
    public boolean supports(SCMSource source) {
        return source instanceof DiversionSCMSource;
    }
    
    @Override
    public boolean supportsDescriptor(jenkins.scm.api.SCMSourceDescriptor descriptor) {
        return descriptor instanceof DiversionSCMSource.DescriptorImpl;
    }
    
    @Override
    public boolean supportsDescriptor(hudson.scm.SCMDescriptor descriptor) {
        return descriptor instanceof DiversionSCM.DescriptorImpl;
    }
    
    @Override
    @CheckForNull
    public SCMFileSystem build(@NonNull Item owner, @NonNull SCM scm, @CheckForNull SCMRevision rev) 
            throws IOException, InterruptedException {
        
        if (!(scm instanceof DiversionSCM)) {
            return null;
        }
        
        DiversionSCM diversionSCM = (DiversionSCM) scm;
        
        // For DiversionSCM used in library loading, use configured library path or repository root
        String libraryPath = diversionSCM.getLibraryPath();
        if (libraryPath == null || libraryPath.isEmpty()) {
            libraryPath = "";  // Root of repository
        }
        
        // Get the configured script path for pipeline script resolution
        String scriptPath = diversionSCM.getScriptPath();
        
        // Create and return the file system with script path for smart resolution
        DiversionSCMFileSystem fs = new DiversionSCMFileSystem(
            diversionSCM.getRepositoryId(),
            diversionSCM.getBranch(),
            diversionSCM.getCredentialsId(),
            libraryPath,
            rev
        );
        
        // Pass job name for auto-detection if no explicit script path
        if (scriptPath == null || scriptPath.isEmpty()) {
            fs.setJobName(owner.getName());
        } else {
            fs.setConfiguredScriptPath(scriptPath);
        }
        
        return fs;
    }
    
    @Override
    @CheckForNull
    public SCMFileSystem build(@NonNull SCMSource source, @NonNull SCMHead head, @CheckForNull SCMRevision rev)
            throws IOException, InterruptedException {
        
        if (!(source instanceof DiversionSCMSource)) {
            return null;
        }
        
        if (!(head instanceof DiversionSCMHead)) {
            return null;
        }
        
        DiversionSCMSource diversionSource = (DiversionSCMSource) source;
        DiversionSCMHead diversionHead = (DiversionSCMHead) head;
        
        // Use the configured library path from the source
        return new DiversionSCMFileSystem(
            diversionSource.getRepositoryId(),
            diversionHead.getBranchId(),
            diversionSource.getCredentialsId(),
            diversionSource.getLibraryPath(),
            rev
        );
    }
}

