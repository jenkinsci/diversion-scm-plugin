package io.superstudios.plugins.diversion;

import hudson.model.Item;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for common UI operations to reduce code duplication.
 * Provides shared methods for populating dropdown lists in Jenkins UI.
 */
public class DiversionUIHelper {
    
    /**
     * Populate credentials dropdown with available StringCredentials.
     * Used by both DiversionSCM and DiversionSCMSource descriptors.
     */
    public static ListBoxModel fillCredentialsIdItems(Item context, String credentialsId) {
        ListBoxModel items = new ListBoxModel();
        
        // Check permissions - USE_ITEM is sufficient (EXTENDED_READ implies it)
        if (context == null) {
            if (!jenkins.model.Jenkins.get().hasPermission(jenkins.model.Jenkins.ADMINISTER)) {
                return items;
            }
        } else {
            if (!context.hasPermission(CredentialsProvider.USE_ITEM)) {
                return items;
            }
        }
        
        items.add("- Select credentials -", "");
        
        // Get all Secret Text credentials
        // Use modern Credentials API pattern: pass null for Authentication and let Jenkins
        // resolve security internally based on the context (Item)
        List<StringCredentials> credentials = 
            CredentialsProvider.lookupCredentials(
                StringCredentials.class,
                context,
                null,  // null auth → Jenkins uses default context
                Collections.emptyList()
            );
        
        // Format each credential to show ID and description
        for (StringCredentials cred : credentials) {
            String id = cred.getId();
            String description = cred.getDescription();
            String displayName = description != null && !description.isEmpty() 
                ? id + " (" + description + ")"
                : id;
            items.add(displayName, id);
        }
        
        return items;
    }
    
    /**
     * Populate repository dropdown with available repositories.
     * Used by both DiversionSCM and DiversionSCMSource descriptors.
     */
    public static ListBoxModel fillRepositoryIdItems(String credentialsId, String errorPrefix) {
        ListBoxModel items = new ListBoxModel();
        
        if (credentialsId == null || credentialsId.isEmpty()) {
            items.add("- Select credentials first -", "");
            return items;
        }
        
        try {
            DiversionApiClient client = new DiversionApiClient(credentialsId);
            List<DiversionRepository> repos = client.listRepositories();
            
            items.add("- Select repository -", "");
            for (DiversionRepository repo : repos) {
                items.add(repo.getName() + " (" + repo.getId() + ")", repo.getId());
            }
        } catch (Exception e) {
            String errorMsg = (errorPrefix != null ? errorPrefix : "Error") + ": " + e.getMessage();
            items.add(errorMsg, "");
        }
        
        return items;
    }
    
    /**
     * Populate branch dropdown with available branches.
     * Used by both DiversionSCM and DiversionSCMSource descriptors.
     */
    public static ListBoxModel fillBranchItems(String credentialsId, String repositoryId, String errorPrefix) {
        ListBoxModel items = new ListBoxModel();
        
        if (credentialsId == null || credentialsId.isEmpty() ||
            repositoryId == null || repositoryId.isEmpty()) {
            items.add("- Select repository first -", "");
            return items;
        }
        
        try {
            DiversionApiClient client = new DiversionApiClient(credentialsId);
            List<DiversionBranch> branches = client.listBranches(repositoryId);
            
            items.add("- Select branch -", "");
            for (DiversionBranch branch : branches) {
                items.add(branch.getName() + " (" + branch.getId() + ")", branch.getId());
            }
        } catch (Exception e) {
            String errorMsg = (errorPrefix != null ? errorPrefix : "Error") + ": " + e.getMessage();
            items.add(errorMsg, "");
        }
        
        return items;
    }
}

