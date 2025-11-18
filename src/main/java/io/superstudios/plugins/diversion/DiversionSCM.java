package io.superstudios.plugins.diversion;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
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
        
        // Auto-detect: search for job name + .groovy anywhere in the repository
        String jobName = build.getParent().getName();
        String expectedFileName = jobName + ".groovy";
        
        try {
            DiversionApiClient client = new DiversionApiClient(credentialsId);
            
            // Get the file tree to search for the script
            List<DiversionFile> files = client.getFileTree(repositoryId, branch);
            
            // Search for the script file anywhere in the repository
            for (DiversionFile file : files) {
                String path = file.getPath();
                if (path.endsWith("/" + expectedFileName) || path.equals(expectedFileName)) {
                    return path;
                }
            }
            
            // Fallback: try common locations
            String[] commonPaths = {
                "Meta/Jenkins/" + expectedFileName,
                "Jenkins/" + expectedFileName,
                "scripts/" + expectedFileName,
                "pipeline/" + expectedFileName,
                expectedFileName
            };
            
            for (String path : commonPaths) {
                try {
                    // Try to get the file content to see if it exists
                    client.getFileContent(repositoryId, branch, path);
                    return path; // File exists, return this path
                } catch (Exception e) {
                    // File doesn't exist at this path, try next
                    continue;
                }
            }
            
        } catch (Exception e) {
            // If search fails, fall back to simple name
        }
        
        // Final fallback: just use the job name
        return expectedFileName;
    }
    
    @Override
    public void checkout(@NonNull Run<?, ?> build, @NonNull Launcher launcher, 
                        @NonNull FilePath workspace, @NonNull TaskListener listener, 
                        File changelogFile, @NonNull SCMRevisionState baseline) throws IOException, InterruptedException {
        
        listener.getLogger().println("Checking out from Diversion repository: " + repositoryId);
        listener.getLogger().println("Using credentials ID: " + credentialsId);
        
        try {
            if (credentialsId == null || credentialsId.trim().isEmpty()) {
                throw new IOException("Credentials ID is null or empty. Please configure Diversion credentials in Jenkins.");
            }
            
            DiversionApiClient client = new DiversionApiClient(credentialsId);
            
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
                            targetFile.getParent().mkdirs();
                            targetFile.write(content, "UTF-8");
                            downloadedCount++;
                        } catch (Exception e) {
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
                targetFile.getParent().mkdirs();
                targetFile.write(content, "UTF-8");
                listener.getLogger().println("Downloaded: " + scriptPath);
            }
            
            // Write changelog file if provided
            // Use a flag to ensure we only write changelog once per build (prefer script checkout over library)
            if (changelogFile != null) {
                boolean shouldWriteChangelog = false;
                
                // Check if this is the first checkout for this build
                synchronized (build) {
                    String changelogKey = "diversion.changelog.written";
                    Object changelogWritten = build.getAction(hudson.model.ParametersAction.class);
                    
                    // Use build number + hash as a simple way to track if we've written changelog
                    String buildKey = build.getExternalizableId();
                    String alreadyWritten = System.getProperty(buildKey + ".changelog");
                    
                    if (alreadyWritten == null) {
                        // First checkout for this build
                        if (!isLibraryCheckout) {
                            // Script checkout - always write changelog
                            shouldWriteChangelog = true;
                            System.setProperty(buildKey + ".changelog", "script");
                            listener.getLogger().println("Creating changelog (script checkout)...");
                        } else {
                            // Library checkout - only write if it's the first checkout
                            shouldWriteChangelog = true;
                            System.setProperty(buildKey + ".changelog", "library");
                            listener.getLogger().println("Creating changelog (library checkout)...");
                        }
                    } else if (alreadyWritten.equals("library") && !isLibraryCheckout) {
                        // We wrote changelog for library, but now we have a script checkout
                        // Overwrite with script checkout (preferred)
                        shouldWriteChangelog = true;
                        System.setProperty(buildKey + ".changelog", "script");
                        listener.getLogger().println("Updating changelog (script checkout - preferred over library)...");
                    } else {
                        listener.getLogger().println("Skipping changelog (already written for this build)");
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
                                } catch (Exception e) {
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
                        java.io.FileWriter writer = new java.io.FileWriter(changelogFile);
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
                        writer.close();
                        
                        listener.getLogger().println("Changelog file created with " + commits.size() + " commits");
                    } catch (Exception e) {
                        listener.getLogger().println("Warning: Could not create changelog file: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            listener.getLogger().println("Checkout completed successfully");
            
            // For Pipeline jobs, create SCM checkout action for change detection
            // This is what enables the "Changes" section to work
            try {
                if (build.getClass().getName().contains("WorkflowRun")) {
                    // Use reflection to avoid dependency issues
                    Object workflowRun = build;
                    Class<?> workflowRunClass = workflowRun.getClass();
                    
                    // Create SCMCheckout action
                    Class<?> scmCheckoutClass = Class.forName("org.jenkinsci.plugins.workflow.job.WorkflowRun$SCMCheckout");
                    Object scmCheckout = scmCheckoutClass.getConstructor(SCM.class).newInstance(this);
                    
                    // Add the action to the build
                    java.util.List<Object> actions = (java.util.List<Object>) workflowRunClass.getMethod("getActions").invoke(workflowRun);
                    actions.add(scmCheckout);
                    
                    listener.getLogger().println("Created SCM checkout action for change detection");
                }
            } catch (Exception e) {
                // If we can't create the SCM checkout action, that's okay
                // The build will still work, just without change detection
                listener.getLogger().println("Note: Could not create SCM checkout action: " + e.getMessage());
            }
            
        } catch (Exception e) {
            listener.getLogger().println("Error during checkout: " + e.getMessage());
            if (e.getCause() != null) {
                listener.getLogger().println("Caused by: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            throw new IOException("Failed to checkout from Diversion: " + e.getMessage(), e);
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
    
    @Override
    public hudson.scm.ChangeLogParser createChangeLogParser() {
        return new DiversionChangeLogParser();
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
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, 
                                                     @QueryParameter String credentialsId) {
            // Check CONFIGURE permission for legacy SCM
            if (context == null || !context.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            return DiversionUIHelper.fillCredentialsIdItems(context, credentialsId);
        }
        
        /**
         * Populate repository dropdown
         */
        public ListBoxModel doFillRepositoryIdItems(@AncestorInPath Item context,
                                                    @QueryParameter String credentialsId) {
            return DiversionUIHelper.fillRepositoryIdItems(credentialsId, "Error");
        }
        
        /**
         * Populate branch dropdown
         */
        public ListBoxModel doFillBranchItems(@AncestorInPath Item context,
                                              @QueryParameter String credentialsId,
                                              @QueryParameter String repositoryId) {
            return DiversionUIHelper.fillBranchItems(credentialsId, repositoryId, "Error");
        }
        
        /**
         * Populate script path dropdown with pipeline scripts
         */
        public ListBoxModel doFillScriptPathItems(@AncestorInPath Item context,
                                                  @QueryParameter String credentialsId,
                                                  @QueryParameter String repositoryId,
                                                  @QueryParameter String branch) {
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
