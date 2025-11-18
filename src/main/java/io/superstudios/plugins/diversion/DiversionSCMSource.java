package io.superstudios.plugins.diversion;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.scm.api.SCMFile;
import jenkins.scm.api.SCMFileSystem;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.jenkinsci.Symbol;
import hudson.model.Item;
import jenkins.model.Jenkins;
import com.cloudbees.plugins.credentials.CredentialsProvider;

import java.io.IOException;
import java.util.List;

/**
 * SCMSource implementation for Diversion repositories - enables Global Pipeline Libraries.
 * This class allows Jenkins to load shared library files (vars/, src/, resources/) from Diversion.
 * 
 * The key to appearing in the Modern SCM dropdown is implementing the retrieve(String, TaskListener) method,
 * which workflow-cps-global-lib's getSCMDescriptors() checks for via hudson.Util.isOverridden().
 */
public class DiversionSCMSource extends SCMSource {
    
    private final String repositoryId;
    private final String credentialsId;
    private String libraryPath = "vars"; // Default Jenkins library structure
    private String defaultBranch = "dev";
    
    @DataBoundConstructor
    public DiversionSCMSource(String repositoryId, String credentialsId) {
        this.repositoryId = repositoryId;
        this.credentialsId = credentialsId;
    }
    
    public String getRepositoryId() {
        return repositoryId;
    }
    
    public String getCredentialsId() {
        return credentialsId;
    }
    
    public String getLibraryPath() {
        return libraryPath;
    }
    
    @DataBoundSetter
    public void setLibraryPath(String libraryPath) {
        this.libraryPath = libraryPath;
    }
    
    public String getDefaultBranch() {
        return defaultBranch;
    }
    
    @DataBoundSetter
    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
    
    /**
     * Required method for Jenkins Global Libraries dropdown visibility.
     * This is what workflow-cps-global-lib's getSCMDescriptors() checks for.
     */
    @Override
    @NonNull
    public SCMRevision retrieve(@NonNull String version, @NonNull TaskListener listener) 
            throws IOException, InterruptedException {
        listener.getLogger().println("Retrieving Diversion revision: " + version);
        
        // Resolve branch name/ID
        String branchId = version.startsWith("dv.branch.") ? version : defaultBranch;
        
        DiversionApiClient client = new DiversionApiClient(credentialsId);
        DiversionBranch branch = client.getBranchDetails(repositoryId, branchId);
        
        DiversionSCMHead head = new DiversionSCMHead(branch.getBranchName(), branch.getBranchId());
        String commitId = branch.getCommitId();
        
        listener.getLogger().println("Branch: " + branch.getBranchName() + " at commit: " + commitId);
        
        return new DiversionSCMRevision(head, commitId != null ? commitId : "HEAD");
    }
    
    @Override
    @CheckForNull
    protected SCMRevision retrieve(@NonNull SCMHead head, @NonNull TaskListener listener) 
            throws IOException, InterruptedException {
        listener.getLogger().println("Retrieving revision for branch: " + head.getName());
        
        DiversionApiClient client = new DiversionApiClient(credentialsId);
        DiversionSCMHead diversionHead = (DiversionSCMHead) head;
        
        try {
            // Get branch details to get current commit
            DiversionBranch branch = client.getBranchDetails(repositoryId, diversionHead.getBranchId());
            String commitId = branch.getCommitId();
            
            if (commitId != null && !commitId.isEmpty()) {
                listener.getLogger().println("Current commit: " + commitId);
                return new DiversionSCMRevision(head, commitId);
            }
        } catch (Exception e) {
            listener.getLogger().println("Failed to retrieve revision: " + e.getMessage());
        }
        
        return null;
    }
    
    
    @Override
    protected void retrieve(@CheckForNull SCMSourceCriteria criteria,
                          @NonNull SCMHeadObserver observer,
                          @CheckForNull SCMHeadEvent<?> event,
                          @NonNull TaskListener listener) throws IOException, InterruptedException {
        
        listener.getLogger().println("Discovering branches in Diversion repository: " + repositoryId);
        
        DiversionApiClient client = new DiversionApiClient(credentialsId);
        
        try {
            // Get repository details
            DiversionRepository repo = client.getRepository(repositoryId);
            listener.getLogger().println("Repository: " + repo.getName());
            
            // Resolve default branch to ID if needed
            String branchId = client.resolveBranchId(repositoryId, defaultBranch);
            listener.getLogger().println("Using branch: " + defaultBranch + " (ID: " + branchId + ")");
            
            // For simplicity, we'll only use the configured default branch
            // This matches the user's requirement for single-branch support
            DiversionSCMHead head = new DiversionSCMHead(defaultBranch, branchId);
            
            // Get current commit for this branch
            DiversionBranch branch = client.getBranchDetails(repositoryId, branchId);
            DiversionSCMRevision revision = new DiversionSCMRevision(head, branch.getCommitId());
            
            // Check if this head meets the criteria
            if (criteria != null) {
                // Build a file system to check if library files exist
                SCMFileSystem fs = new DiversionSCMFileSystem(
                    repositoryId, branchId, credentialsId, libraryPath, revision);
                if (fs != null) {
                    SCMFile root = fs.getRoot();
                    if (!criteria.isHead(new SCMSourceCriteria.Probe() {
                        @Override
                        public String name() {
                            return head.getName();
                        }
                        
                        @Override
                        public long lastModified() {
                            return 0;
                        }
                        
                        @Override
                        public boolean exists(@NonNull String path) throws IOException {
                            try {
                                SCMFile file = root.child(path);
                                // Use isFile() or isDirectory() instead of type() which is protected
                                return file.isFile() || file.isDirectory();
                            } catch (InterruptedException e) {
                                throw new IOException(e);
                            }
                        }
                    }, listener)) {
                        listener.getLogger().println("Branch does not meet criteria");
                        return;
                    }
                }
            }
            
            // Observe the head
            observer.observe(head, revision);
            listener.getLogger().println("Branch discovery completed");
            
        } catch (Exception e) {
            listener.getLogger().println("Error during branch discovery: " + e.getMessage());
            e.printStackTrace(listener.getLogger());
            throw new IOException("Failed to discover branches: " + e.getMessage(), e);
        }
    }
    
    @Override
    @NonNull
    public hudson.scm.SCM build(@NonNull SCMHead head, @CheckForNull SCMRevision revision) {
        // Return a DiversionSCM instance for library loading
        // This is required for Jenkins to track the SCM source
        if (!(head instanceof DiversionSCMHead)) {
            throw new IllegalArgumentException("Expected DiversionSCMHead, got " + head.getClass().getName());
        }
        DiversionSCM scm = new DiversionSCM(repositoryId, credentialsId);
        scm.setBranch(((DiversionSCMHead) head).getBranchId());
        scm.setLibraryPath(libraryPath);
        return scm;
    }
    
    @Symbol({"diversionSource", "diversion"})
    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor {
        
        @Override
        @NonNull
        public String getDisplayName() {
            return "Diversion";
        }
        
        @NonNull
        @Override
        public String getId() {
            // Use fully-qualified class name pattern like other SCM plugins
            return "io.superstudios.plugins.diversion.DiversionSCMSource";
        }
        
        /**
         * Populate repository dropdown with available repositories
         */
        @RequirePOST
        public ListBoxModel doFillRepositoryIdItems(@AncestorInPath Item context,
                                                    @QueryParameter String credentialsId) {
            // Check permissions before accessing credentials
            if (context == null) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            } else {
                // User must have at least one of these permissions
                if (!context.hasPermission(Item.EXTENDED_READ)
                    && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return new ListBoxModel();
                }
            }
            return DiversionUIHelper.fillRepositoryIdItems(credentialsId, "Error loading repositories");
        }
        
        /**
         * Populate branch dropdown with available branches
         */
        @RequirePOST
        public ListBoxModel doFillDefaultBranchItems(@AncestorInPath Item context,
                                                     @QueryParameter String credentialsId,
                                                     @QueryParameter String repositoryId) {
            // Check permissions before accessing credentials
            if (context == null) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            } else {
                // User must have at least one of these permissions
                if (!context.hasPermission(Item.EXTENDED_READ)
                    && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return new ListBoxModel();
                }
            }
            return DiversionUIHelper.fillBranchItems(credentialsId, repositoryId, "Error loading branches");
        }
        
        /**
         * Populate library path dropdown with directories from repository
         */
        @RequirePOST
        public ListBoxModel doFillLibraryPathItems(@AncestorInPath Item context,
                                                   @QueryParameter String credentialsId,
                                                   @QueryParameter String repositoryId,
                                                   @QueryParameter String defaultBranch,
                                                   @QueryParameter String libraryPath) {
            // Check permissions before accessing credentials
            if (context == null) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            } else {
                // User must have at least one of these permissions
                if (!context.hasPermission(Item.EXTENDED_READ)
                    && !context.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return new ListBoxModel();
                }
            }
            
            ListBoxModel items = new ListBoxModel();
            
            // Always include current value first
            if (libraryPath != null && !libraryPath.isEmpty()) {
                items.add(libraryPath, libraryPath);
            }
            
            // Root option
            items.add("Root (vars/ at repository root)", "");
            
            // If credentials and repo are selected, try to list actual directories
            if (credentialsId != null && !credentialsId.isEmpty() &&
                repositoryId != null && !repositoryId.isEmpty() &&
                defaultBranch != null && !defaultBranch.isEmpty()) {
                
                try {
                    DiversionApiClient client = new DiversionApiClient(credentialsId);
                    List<DiversionFile> files = client.getFileTree(repositoryId, defaultBranch);
                    
                    // Find directories that contain a "vars" subdirectory with .groovy files
                    // This indicates they are valid Jenkins shared library paths
                    java.util.Set<String> varsParents = new java.util.HashSet<>();
                    boolean hasRootVars = false;
                    
                    for (DiversionFile file : files) {
                        String path = file.getPath();
                        
                        // Check if this is a .groovy file inside a vars/ directory
                        if (path.contains("/vars/") && path.endsWith(".groovy")) {
                            int varsIndex = path.indexOf("/vars/");
                            if (varsIndex > 0) {
                                // Extract parent directory of vars (e.g., "Meta/Jenkins/SharedLib")
                                String parent = path.substring(0, varsIndex);
                                varsParents.add(parent);
                            } else if (varsIndex == 0) {
                                // vars/ at root level
                                hasRootVars = true;
                            }
                        }
                    }
                    
                    // Show detected library paths
                    if (!varsParents.isEmpty() || hasRootVars) {
                        items.add("- Detected Library Paths -", "");
                        
                        if (hasRootVars) {
                            items.add("📚 Repository Root (vars/ at root)", "");
                        }
                        
                        java.util.List<String> sortedPaths = new java.util.ArrayList<>(varsParents);
                        java.util.Collections.sort(sortedPaths);
                        for (String path : sortedPaths) {
                            items.add("📚 " + path, path);
                        }
                    }
                } catch (Exception e) {
                    items.add("(Could not load directories: " + e.getMessage() + ")", "");
                }
            }
            
            return items;
        }
        
        /**
         * Populate credentials dropdown
         */
        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context,
                                                     @QueryParameter String credentialsId) {
            return DiversionUIHelper.fillCredentialsIdItems(context, credentialsId);
        }
    }
}

