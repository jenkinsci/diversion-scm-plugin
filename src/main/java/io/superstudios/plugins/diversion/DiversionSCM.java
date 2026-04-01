package io.superstudios.plugins.diversion;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMRevision;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Jenkins SCM implementation for Diversion repositories.
 * 
 * This SCM allows Jenkins to checkout files from Diversion repositories
 * using the Diversion API.
 */
public class DiversionSCM extends SCM {
    
    private final String repositoryId;
    private final String credentialsId;
    private String branch = "main";
    private String scriptPath;
    private String libraryPath;
    
    @DataBoundConstructor
    public DiversionSCM(String repositoryId, String credentialsId) {
        this.repositoryId = repositoryId;
        this.credentialsId = credentialsId;
    }
    
    @DataBoundSetter
    public void setBranch(String branch) {
        this.branch = branch;
    }
    
    @DataBoundSetter
    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }
    
    @DataBoundSetter
    public void setLibraryPath(String libraryPath) {
        this.libraryPath = libraryPath;
    }
    
    public String getRepositoryId() {
        return repositoryId;
    }
    
    public String getCredentialsId() {
        return credentialsId;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public String getScriptPath() {
        return scriptPath;
    }
    
    public String getLibraryPath() {
        return libraryPath;
    }
    
    /**
     * Get the script path from the Jenkins job configuration
     * This method is only used when no explicit scriptPath is configured.
     * Most users will configure the scriptPath directly in their job.
     */
    private String getScriptPathFromJob(Run<?, ?> build) {
        // If scriptPath is explicitly configured, use it
        if (scriptPath != null && !scriptPath.isEmpty()) {
            return scriptPath;
        }
        
        // Auto-detect: search for job name with various patterns
        String jobName = build.getParent().getName();
        
        // Try multiple file name patterns based on job name
        String[] expectedFileNames = {
            jobName + ".groovy",  // e.g., "simple-test.groovy"
            jobName,               // e.g., "Jenkinsfile-demo" (no extension)
            "Jenkinsfile"          // Standard Jenkinsfile anywhere in repo
        };
        
        try {
            DiversionApiClient client = new DiversionApiClient(credentialsId);
            
            // Get the file tree to search for the script
            List<DiversionFile> files = client.getFileTree(repositoryId, branch);
            
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
            // If search fails, fall back to simple name
        }
        
        // Final fallback: just use the job name + .groovy
        return jobName + ".groovy";
    }
    
    @Override
    public void checkout(@NonNull Run<?, ?> build, @NonNull Launcher launcher, 
                        @NonNull FilePath workspace, @NonNull TaskListener listener, 
                        File changelogFile, @CheckForNull SCMRevisionState baseline) throws IOException, InterruptedException {
        
        listener.getLogger().println("Checking out from Diversion repository: " + repositoryId);
        listener.getLogger().println("Using credentials ID: " + credentialsId);
        
        try {
            if (credentialsId == null || credentialsId.trim().isEmpty()) {
                throw new IOException("Credentials ID is null or empty. Please configure Diversion credentials in Jenkins.");
            }
            
            // Track credential usage for reporting
            // Use findCredentialById with Run context (supports folder-scoped credentials)
            StandardCredentials credentials = CredentialsProvider.findCredentialById(
                credentialsId,
                StandardCredentials.class,
                build,
                Collections.emptyList()
            );
            if (credentials != null) {
                CredentialsProvider.track(build, credentials);
            }
            
            // Create client with Run context for proper credential resolution
            DiversionApiClient client = new DiversionApiClient(credentialsId, build);
            
            // Get repository details
            listener.getLogger().println("Getting repository details...");
            DiversionRepository repo = client.getRepository(repositoryId);
            listener.getLogger().println("Repository: " + repo.getName());
            
            // Check if this is a library checkout (workspace path contains @libs)
            String workspacePath = workspace.getRemote();
            boolean isLibraryCheckout = workspacePath.contains("@libs");
            
            if (isLibraryCheckout) {
                // Library checkout - download all files from the library path
                listener.getLogger().println("Library checkout detected - downloading library files");
                
                // Use configured library path or default
                String libPath = (libraryPath != null && !libraryPath.isEmpty()) 
                    ? libraryPath 
                    : "Meta/Jenkins/SharedLibs";
                listener.getLogger().println("Using library path: " + libPath);
                
                java.util.List<DiversionFile> files = client.getFileTree(repositoryId, branch);
                
                int downloadedCount = 0;
                for (DiversionFile file : files) {
                    String filePath = file.getPath();
                    // Only download files from the library directory
                    if (filePath != null && filePath.startsWith(libPath + "/")) {
                        try {
                            String content = client.getFileContent(repositoryId, branch, filePath);
                            // Remove the library path prefix so files are at workspace root
                            String relativePath = filePath.substring(libPath.length() + 1);
                            FilePath targetFile = workspace.child(relativePath);
                            FilePath parent = targetFile.getParent();
                            if (parent != null) {
                                parent.mkdirs();
                            }
                            targetFile.write(content, "UTF-8");
                            downloadedCount++;
                        } catch (IOException | InterruptedException e) {
                            listener.getLogger().println("Warning: Could not download " + filePath + ": " + e.getMessage());
                        }
                    }
                }
                listener.getLogger().println("Downloaded " + downloadedCount + " library files");
                
            } else {
                // Pipeline script checkout - download only the script file
                String scriptPath = getScriptPathFromJob(build);
                listener.getLogger().println("Script path: " + scriptPath);
                
                // Download the specific script file
                String content = client.getFileContent(repositoryId, branch, scriptPath);
                FilePath targetFile = workspace.child(scriptPath);
                FilePath parent = targetFile.getParent();
                if (parent != null) {
                    parent.mkdirs();
                }
                targetFile.write(content, "UTF-8");
                listener.getLogger().println("Downloaded: " + scriptPath);
            }
            
            // Write changelog file if provided
            // Smart duplicate prevention: During library checkout, we check if the pipeline's
            // SCM is configured to use the same Diversion repository. If so, we skip writing
            // the library changelog to prevent duplicates.
            if (changelogFile != null) {
                boolean shouldWriteChangelog = false;
                boolean skipForSameRepo = false;
                
                // Use build ID to track state across checkouts
                String buildKey = build.getExternalizableId();
                
                synchronized (build) {
                    String alreadyWritten = System.getProperty(buildKey + ".changelog");
                    
                    if (alreadyWritten == null) {
                        // First checkout for this build
                        if (!isLibraryCheckout) {
                            // Script checkout - always write changelog
                            shouldWriteChangelog = true;
                            System.setProperty(buildKey + ".changelog", "script");
                            listener.getLogger().println("Creating changelog (script checkout)...");
                        } else {
                            // Library checkout - check if pipeline uses the same repo
                            String pipelineRepoId = getPipelineRepositoryId(build, listener);
                            if (pipelineRepoId != null && pipelineRepoId.equals(repositoryId)) {
                                // Same repo! Skip library changelog to prevent duplicates
                                skipForSameRepo = true;
                                System.setProperty(buildKey + ".changelog", "library-skipped");
                                listener.getLogger().println("Library and pipeline use same repository (" + repositoryId + ") - skipping library changelog to prevent duplicates");
                            } else {
                                // Different repo or couldn't determine - write changelog
                                shouldWriteChangelog = true;
                                System.setProperty(buildKey + ".changelog", "library");
                                if (pipelineRepoId != null) {
                                    listener.getLogger().println("Library uses different repository than pipeline - creating changelog");
                                } else {
                                    listener.getLogger().println("Creating changelog (library checkout)...");
                                }
                            }
                        }
                    } else if ((alreadyWritten.equals("library") || alreadyWritten.equals("library-skipped")) && !isLibraryCheckout) {
                        // Library checked out first, now script checkout
                        shouldWriteChangelog = true;
                        System.setProperty(buildKey + ".changelog", "script");
                        listener.getLogger().println("Creating changelog (script checkout)...");
                    } else {
                        listener.getLogger().println("Skipping changelog (already written for this build)");
                    }
                }
                
                // Write empty changelog if we're skipping for same-repo
                if (skipForSameRepo) {
                    try (java.io.FileWriter writer = new java.io.FileWriter(changelogFile, java.nio.charset.StandardCharsets.UTF_8)) {
                        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<changelog>\n</changelog>\n");
                    }
                }
                
                if (shouldWriteChangelog) {
                    try {
                        // Get the latest commit for this build
                        DiversionCommit latestCommit = client.getLatestCommit(repositoryId, branch);
                        String currentCommitId = latestCommit.getCommitId();
                        
                        // Get the previous build's commit ID (if any)
                        String previousCommitId = null;
                        Run<?, ?> previousBuild = build.getPreviousBuild();
                        if (previousBuild != null) {
                            SCMRevisionState previousState = previousBuild.getAction(SCMRevisionState.class);
                            if (previousState != null && previousState.getClass().getName().contains("MultiSCMRevisionState")) {
                                // Extract Diversion state from MultiSCMRevisionState using reflection
                                try {
                                    java.lang.reflect.Field statesField = previousState.getClass().getDeclaredField("revisionStates");
                                    statesField.setAccessible(true);
                                    java.util.Map<?, ?> states = (java.util.Map<?, ?>) statesField.get(previousState);
                                    for (Object state : states.values()) {
                                        if (state instanceof DiversionSCMRevisionState) {
                                            previousCommitId = ((DiversionSCMRevisionState) state).getCommitId();
                                            break;
                                        }
                                    }
                                } catch (ReflectiveOperationException e) {
                                    // Couldn't extract previous commit, will show all recent commits
                                }
                            } else if (previousState instanceof DiversionSCMRevisionState) {
                                // Direct Diversion state (non-pipeline jobs)
                                previousCommitId = ((DiversionSCMRevisionState) previousState).getCommitId();
                            }
                        }
                        
                        // Get commits to include in changelog
                        java.util.List<DiversionCommit> commits = new java.util.ArrayList<>();
                        
                        if (previousCommitId != null && previousCommitId.equals(currentCommitId)) {
                            // No changes between builds
                            listener.getLogger().println("No new commits since last build");
                        } else if (previousCommitId != null) {
                            // Get commits between previous and current
                            listener.getLogger().println("Finding commits between " + previousCommitId + " and " + currentCommitId);
                            java.util.List<DiversionCommit> allCommits = client.listCommits(repositoryId, 100);
                            
                            // Add commits from newest to the previous commit
                            boolean foundCurrent = false;
                            for (DiversionCommit commit : allCommits) {
                                if (commit.getCommitId().equals(currentCommitId)) {
                                    foundCurrent = true;
                                }
                                if (foundCurrent) {
                                    if (commit.getCommitId().equals(previousCommitId)) {
                                        break; // Stop at previous commit
                                    }
                                    commits.add(commit);
                                }
                            }
                            listener.getLogger().println("Found " + commits.size() + " new commits");
                        } else {
                            // First build - just show the latest commit
                            listener.getLogger().println("First build - showing latest commit");
                            commits.add(latestCommit);
                        }
                        
                        // Write changelog as XML
                        try (java.io.FileWriter writer = new java.io.FileWriter(changelogFile, java.nio.charset.StandardCharsets.UTF_8)) {
                        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                        writer.write("<changelog>\n");
                        
                        for (DiversionCommit commit : commits) {
                            writer.write("  <entry>\n");
                            writer.write("    <commitId>" + escapeXml(commit.getCommitId()) + "</commitId>\n");
                            writer.write("    <msg>" + escapeXml(commit.getCommitMessage()) + "</msg>\n");
                            writer.write("    <author>" + escapeXml(commit.getAuthor().getName()) + "</author>\n");
                            writer.write("    <timestamp>" + commit.getCreatedTs() + "</timestamp>\n");
                            
                            // Add changed files if available
                            if (commit.getChangedFiles() != null && !commit.getChangedFiles().isEmpty()) {
                                writer.write("    <files>\n");
                                for (String file : commit.getChangedFiles()) {
                                    writer.write("      <file>" + escapeXml(file) + "</file>\n");
                                }
                                writer.write("    </files>\n");
                            }
                            
                            writer.write("  </entry>\n");
                        }
                        
                        writer.write("</changelog>\n");
                        }
                        
                        listener.getLogger().println("Changelog file created with " + commits.size() + " commits");
                    } catch (IOException e) {
                        listener.getLogger().println("Warning: Could not create changelog file: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            listener.getLogger().println("Checkout completed successfully");
            
        } catch (IOException | InterruptedException e) {
            listener.getLogger().println("Error during checkout: " + e.getMessage());
            if (e.getCause() != null) {
                listener.getLogger().println("Caused by: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            if (e instanceof IOException) {
                throw (IOException) e;
            } else {
            throw new IOException("Failed to checkout from Diversion: " + e.getMessage(), e);
            }
        }
    }
    
    @Override
    public SCMRevisionState calcRevisionsFromBuild(@NonNull Run<?, ?> build, @NonNull FilePath workspace, 
                                                  @NonNull Launcher launcher, @NonNull TaskListener listener) 
                                                  throws IOException, InterruptedException {
        try {
            DiversionApiClient client = new DiversionApiClient(credentialsId);
            DiversionCommit latestCommit = client.getLatestCommit(repositoryId, branch);
            
            // Create a revision state that tracks the commit
            return new DiversionSCMRevisionState(latestCommit.getCommitId(), latestCommit.getCreatedTs());
        } catch (Exception e) {
            listener.getLogger().println("Warning: Could not calculate revision state: " + e.getMessage());
            return new SCMRevisionState() {};
        }
    }
    
    /**
     * Compare the remote repository state with the last built revision.
     * This is the core method called during SCM polling to detect changes.
     * 
     * @param project The job being polled
     * @param launcher The launcher
     * @param workspace The workspace (may be null for lightweight checkout)
     * @param listener For logging
     * @param baseline The revision state from the last build
     * @return PollingResult indicating whether changes were found
     */
    @Override
    public PollingResult compareRemoteRevisionWith(@NonNull Job<?, ?> project,
                                                   @CheckForNull Launcher launcher,
                                                   @CheckForNull FilePath workspace,
                                                   @NonNull TaskListener listener,
                                                   @NonNull SCMRevisionState baseline)
                                                   throws IOException, InterruptedException {
        
        listener.getLogger().println("Polling Diversion repository: " + repositoryId + " (branch: " + branch + ")");
        
        try {
            DiversionApiClient client = new DiversionApiClient(credentialsId);
            DiversionCommit latestCommit = client.getLatestCommit(repositoryId, branch);
            String remoteCommitId = latestCommit.getCommitId();
            
            listener.getLogger().println("Latest remote commit: " + remoteCommitId);
            
            // Check if we have a valid baseline to compare against
            if (baseline instanceof DiversionSCMRevisionState) {
                DiversionSCMRevisionState diversionBaseline = (DiversionSCMRevisionState) baseline;
                String baselineCommitId = diversionBaseline.getCommitId();
                
                listener.getLogger().println("Last built commit: " + baselineCommitId);
                
                if (remoteCommitId.equals(baselineCommitId)) {
                    listener.getLogger().println("No changes detected");
                    return PollingResult.NO_CHANGES;
                } else {
                    listener.getLogger().println("Changes detected! Triggering build.");
                    return PollingResult.SIGNIFICANT;
                }
            } else {
                // No valid baseline (first build or different SCM type)
                listener.getLogger().println("No valid baseline found - treating as changed");
                return PollingResult.BUILD_NOW;
            }
            
        } catch (Exception e) {
            listener.getLogger().println("Error during polling: " + e.getMessage());
            e.printStackTrace(listener.getLogger());
            // Don't trigger builds on error - better to fail silently than spam builds
            return PollingResult.NO_CHANGES;
        }
    }
    
    /**
     * Tell Jenkins we support polling.
     */
    @Override
    public boolean supportsPolling() {
        return true;
    }
    
    /**
     * Tell Jenkins we don't need a workspace for polling.
     * We only need API access to check for new commits.
     */
    @Override
    public boolean requiresWorkspaceForPolling() {
        return false;
    }
    
    @Override
    public hudson.scm.ChangeLogParser createChangeLogParser() {
        return new DiversionChangeLogParser();
    }
    
    /**
     * Get the repository ID configured for the pipeline's SCM (if it's a DiversionSCM).
     * This allows us to detect when a library checkout is from the same repo as the pipeline,
     * so we can skip writing duplicate changelog entries.
     * 
     * @param build The current build
     * @param listener For logging
     * @return The repository ID if the pipeline uses DiversionSCM, null otherwise
     */
    private String getPipelineRepositoryId(Run<?, ?> build, TaskListener listener) {
        try {
            Job<?, ?> job = build.getParent();
            
            // Check if this is a WorkflowJob (pipeline)
            if (job.getClass().getName().equals("org.jenkinsci.plugins.workflow.job.WorkflowJob")) {
                // Use reflection to access the flow definition
                java.lang.reflect.Method getDefinition = job.getClass().getMethod("getDefinition");
                Object definition = getDefinition.invoke(job);
                
                if (definition != null && definition.getClass().getName().equals("org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition")) {
                    // This is a "Pipeline script from SCM" job
                    java.lang.reflect.Method getScm = definition.getClass().getMethod("getScm");
                    Object scm = getScm.invoke(definition);
                    
                    if (scm instanceof DiversionSCM) {
                        String pipelineRepoId = ((DiversionSCM) scm).getRepositoryId();
                        listener.getLogger().println("Pipeline is configured with Diversion repository: " + pipelineRepoId);
                        return pipelineRepoId;
                    }
                }
            }
        } catch (Exception e) {
            // Couldn't determine pipeline SCM - not a problem, we'll just write the changelog
            listener.getLogger().println("Note: Could not determine pipeline SCM type: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Escape XML special characters
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    @Extension
    public static class DescriptorImpl extends SCMDescriptor<DiversionSCM> {
        
        public DescriptorImpl() {
            super(DiversionSCM.class, null);
        }
        
        @Override
        public String getDisplayName() {
            return "Diversion";
        }
        
        @Override
        public boolean isApplicable(Job project) {
            return true;
        }
        
        /**
         * Get list of available credentials for Diversion API tokens (Secret Text type)
         */
        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, 
                                                     @QueryParameter String credentialsId) {
            // Permission check is handled in DiversionUIHelper
            return DiversionUIHelper.fillCredentialsIdItems(context, credentialsId);
        }
        
        /**
         * Populate repository dropdown
         */
        @RequirePOST
        public ListBoxModel doFillRepositoryIdItems(@AncestorInPath Item context,
                                                    @QueryParameter String credentialsId) {
            // Check permissions before accessing credentials
            if (context == null) {
                if (!Jenkins.get().hasPermission(Jenkins.MANAGE)) {
                    return new ListBoxModel();
            }
            } else {
                if (!context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return new ListBoxModel();
                }
            }
            return DiversionUIHelper.fillRepositoryIdItems(credentialsId, "Error");
        }
        
        /**
         * Populate branch dropdown
         */
        @RequirePOST
        public ListBoxModel doFillBranchItems(@AncestorInPath Item context,
                                              @QueryParameter String credentialsId,
                                              @QueryParameter String repositoryId) {
            // Check permissions before accessing credentials
            if (context == null) {
                if (!Jenkins.get().hasPermission(Jenkins.MANAGE)) {
                    return new ListBoxModel();
            }
            } else {
                if (!context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return new ListBoxModel();
                }
            }
            return DiversionUIHelper.fillBranchItems(credentialsId, repositoryId, "Error");
        }
        
        /**
         * Populate script path dropdown with pipeline scripts
         */
        @RequirePOST
        public ListBoxModel doFillScriptPathItems(@AncestorInPath Item context,
                                                  @QueryParameter String credentialsId,
                                                  @QueryParameter String repositoryId,
                                                  @QueryParameter String branch) {
            // Check permissions before accessing credentials
            if (context == null) {
                if (!Jenkins.get().hasPermission(Jenkins.MANAGE)) {
                    return new ListBoxModel();
                }
            } else {
                if (!context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return new ListBoxModel();
                }
            }
            
            ListBoxModel items = new ListBoxModel();
            
            // Default auto-detect option
            items.add("(Auto-detect from job name)", "");
            
            // If all fields are filled, try to list actual Groovy files
            if (credentialsId != null && !credentialsId.isEmpty() &&
                repositoryId != null && !repositoryId.isEmpty() &&
                branch != null && !branch.isEmpty()) {
                
                try {
                    DiversionApiClient client = new DiversionApiClient(credentialsId);
                    List<DiversionFile> files = client.getFileTree(repositoryId, branch);
                    
                    items.add("- Pipeline Scripts -", "");
                    for (DiversionFile file : files) {
                        // Only show .groovy files
                        if (file.getPath().endsWith(".groovy")) {
                            items.add("📄 " + file.getPath(), file.getPath());
                        }
                    }
                } catch (Exception e) {
                    items.add("(Could not load files: " + e.getMessage() + ")", "");
                }
            }
            
            return items;
        }
    }
    
}
