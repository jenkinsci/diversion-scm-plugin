package io.superstudios.plugins.diversion;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;

import java.io.IOException;

/**
 * File system implementation for accessing Diversion repository files.
 * Used by Jenkins to load library files from Diversion.
 */
public class DiversionSCMFileSystem extends SCMFileSystem {
    
    private final String repositoryId;
    private final String branchId;
    private final String credentialsId;
    private final String libraryPath;
    private final DiversionApiClient apiClient;
    private Long cachedLastModified;
    
    // For smart script path resolution
    private String jobName;
    private String configuredScriptPath;
    
    public DiversionSCMFileSystem(String repositoryId, String branchId, 
                                  String credentialsId, String libraryPath,
                                  SCMRevision revision) 
            throws IOException, InterruptedException {
        super(revision);
        this.repositoryId = repositoryId;
        this.branchId = branchId;
        this.credentialsId = credentialsId;
        this.libraryPath = libraryPath;
        this.apiClient = new DiversionApiClient(credentialsId);
        this.cachedLastModified = null;
    }
    
    @Override
    @NonNull
    public SCMFile getRoot() {
        // Return the library base path which should contain vars/, src/, resources/
        // If libraryPath is empty or "", it means root of repository
        String rootPath = (libraryPath == null || libraryPath.isEmpty()) ? "" : libraryPath;
        return new DiversionSCMFile(this, rootPath, apiClient, repositoryId, branchId);
    }
    
    @Override
    public long lastModified() throws IOException, InterruptedException {
        // Return the actual commit timestamp to enable proper change detection
        // This ensures Jenkins reloads libraries when there are new commits
        if (cachedLastModified == null) {
            try {
                // Get the latest commit for this branch to check its timestamp
                DiversionBranch branch = apiClient.getBranchDetails(repositoryId, branchId);
                String commitId = branch.getCommitId();
                
                if (commitId != null && !commitId.isEmpty()) {
                    // Get commit details to get the actual timestamp
                    DiversionCommit commit = apiClient.getCommitDetails(repositoryId, commitId);
                    // Convert from seconds to milliseconds
                    cachedLastModified = commit.getCreatedTs() * 1000;
                } else {
                    // Fallback to current time if we can't get commit info
                    cachedLastModified = System.currentTimeMillis();
                }
            } catch (IOException | InterruptedException e) {
                // If we can't get commit info, return current time
                // This ensures libraries are reloaded on next check
                cachedLastModified = System.currentTimeMillis();
            }
        }
        return cachedLastModified;
    }
    
    /**
     * Invalidate the cached last modified time.
     * Call this when we want to force a refresh on the next lastModified() call.
     */
    public void invalidateCache() {
        cachedLastModified = null;
    }
    
    @Override
    public void close() throws IOException {
        // No resources to close
    }
    
    public String getRepositoryId() {
        return repositoryId;
    }
    
    public String getBranchId() {
        return branchId;
    }
    
    public String getCredentialsId() {
        return credentialsId;
    }
    
    public String getLibraryPath() {
        return libraryPath;
    }
    
    public DiversionApiClient getApiClient() {
        return apiClient;
    }
    
    /**
     * Set the job name for auto-detecting script path.
     * When Jenkins asks for "Jenkinsfile" but it doesn't exist,
     * we'll look for {jobName}.groovy instead.
     */
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
    
    public String getJobName() {
        return jobName;
    }
    
    /**
     * Set the explicitly configured script path.
     * This takes precedence over job name auto-detection.
     */
    public void setConfiguredScriptPath(String configuredScriptPath) {
        this.configuredScriptPath = configuredScriptPath;
    }
    
    public String getConfiguredScriptPath() {
        return configuredScriptPath;
    }
    
    /**
     * Resolve the actual script path for a requested path.
     * If the requested path is a default like "Jenkinsfile" and doesn't exist,
     * try to find the configured or auto-detected script path.
     * 
     * @param requestedPath The path Jenkins is asking for (e.g., "Jenkinsfile")
     * @return The actual path to use, or null to use the original
     */
    public String resolveScriptPath(String requestedPath) {
        // If we have an explicit script path configured, use it when Jenkins asks for default paths
        if (configuredScriptPath != null && !configuredScriptPath.isEmpty()) {
            // Check if this is a default Jenkinsfile request
            if (requestedPath.equals("Jenkinsfile") || requestedPath.equals("jenkinsfile")) {
                return configuredScriptPath;
            }
        }
        
        // If we have a job name, try auto-detection
        if (jobName != null && !jobName.isEmpty()) {
            // Check if this is a default Jenkinsfile request
            if (requestedPath.equals("Jenkinsfile") || requestedPath.equals("jenkinsfile")) {
                // Try multiple file name patterns based on job name
                String[] expectedFileNames = {
                    jobName + ".groovy",  // e.g., "simple-test.groovy"
                    jobName,               // e.g., "Jenkinsfile-demo" (no extension)
                    "Jenkinsfile"          // Standard Jenkinsfile anywhere in repo
                };
                
                try {
                    // Search for the script file anywhere in the repository
                    java.util.List<DiversionFile> files = apiClient.getFileTree(repositoryId, branchId);
                    
                    // Try each pattern in order of preference
                    for (String expectedFileName : expectedFileNames) {
                        for (DiversionFile file : files) {
                            String path = file.getPath();
                            if (path != null && (path.endsWith("/" + expectedFileName) || path.equals(expectedFileName))) {
                                return path;
                            }
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    // Auto-detection failed, will fall back to original path
                }
            }
        }
        
        // No resolution needed, use original path
        return null;
    }
}

