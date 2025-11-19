package io.superstudios.plugins.diversion;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import hudson.model.Item;
import hudson.model.Run;
import hudson.ProxyConfiguration;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client for interacting with Diversion API.
 * 
 * Based on Diversion API documentation: https://docs.diversion.dev/api-reference/introduction
 */
public class DiversionApiClient {
    
    private static final String API_BASE_URL = "https://api.diversion.dev/v0";
    private static final String AUTH_BASE_URL = "https://auth.diversion.dev/oauth2/token";
    private static final String CLIENT_ID = "j084768v4hd6j1pf8df4h4c47";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String credentialsId;
    private final HttpClient httpClient;
    private final Run<?, ?> run; // Run context for credential lookup (supports folder-scoped credentials)
    
    /**
     * Constructor for use during a Run (build execution)
     * @param credentialsId The credential ID to use
     * @param run The Run context (used to resolve folder-scoped credentials)
     */
    public DiversionApiClient(String credentialsId, Run<?, ?> run) {
        this.credentialsId = credentialsId;
        this.run = run;
        this.httpClient = createHttpClient();
    }
    
    /**
     * Constructor for use outside of a Run context (e.g., UI dropdowns)
     * @param credentialsId The credential ID to use
     */
    public DiversionApiClient(String credentialsId) {
        this.credentialsId = credentialsId;
        this.run = null;
        this.httpClient = createHttpClient();
    }
    
    /**
     * Create HttpClient with proxy support from Jenkins ProxyConfiguration
     */
    private HttpClient createHttpClient() {
        Jenkins jenkins = Jenkins.get();
        ProxyConfiguration proxyConfig = jenkins.proxy;
        
        if (proxyConfig != null) {
            // Use ProxyConfiguration's built-in method which handles authentication
            return proxyConfig.newHttpClient();
        }
        
        return HttpClient.newHttpClient();
    }
    
    /**
     * Get the API token from Jenkins credentials
     * Uses findCredentialById when ItemGroup context is available (during Run),
     * falls back to lookupCredentials for UI contexts.
     */
    private String getApiToken() throws IOException {
        if (credentialsId == null || credentialsId.trim().isEmpty()) {
            throw new IOException("Diversion credentials ID is null or empty");
        }
        
        StandardCredentials credentials;
        
        if (run != null) {
            // During a Run: use findCredentialById with Run context (supports folder-scoped credentials)
            credentials = CredentialsProvider.findCredentialById(
                credentialsId,
                StringCredentials.class,
                run,
                Collections.emptyList()
            );
        } else {
            // UI context: fallback to lookupCredentials (for backward compatibility)
            credentials = CredentialsProvider.lookupCredentials(
                StandardCredentials.class,
                (hudson.model.ItemGroup<?>) null,
                null,
                Collections.emptyList()
            ).stream()
            .filter(cred -> credentialsId.equals(cred.getId()))
            .findFirst()
            .orElse(null);
        }
        
        if (credentials == null) {
            throw new IOException("Diversion credentials not found: " + credentialsId);
        }
        
        if (credentials instanceof StringCredentials) {
            return Secret.toString(((StringCredentials) credentials).getSecret());
        }
        
        throw new IOException("Invalid credentials type for Diversion API token");
    }
    
    /**
     * Make authenticated request to Diversion API
     */
    private JsonNode makeRequest(String endpoint) throws IOException, InterruptedException {
        String accessToken = getAccessToken();
        String url = API_BASE_URL + endpoint;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IOException("Diversion API request failed: " + response.statusCode() + " - " + response.body());
        }
        
        return objectMapper.readTree(response.body());
    }
    
    /**
     * Exchange refresh token for access token
     */
    private String getAccessToken() throws IOException, InterruptedException {
        String refreshToken = getApiToken();
        
        String requestBody = "grant_type=refresh_token&refresh_token=" + refreshToken + "&client_id=" + CLIENT_ID;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(AUTH_BASE_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IOException("Token exchange failed: " + response.statusCode() + " - " + response.body());
        }
        
        JsonNode jsonResponse = objectMapper.readTree(response.body());
        return jsonResponse.get("access_token").asText();
    }
    
    /**
     * Test authentication with Diversion API
     */
    public boolean testAuthentication() throws IOException, InterruptedException {
        try {
            String accessToken = getAccessToken();
            return accessToken != null && !accessToken.isEmpty();
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Get repository details
     */
    public DiversionRepository getRepository(String repositoryId) throws IOException, InterruptedException {
        JsonNode response = makeRequest("/repos/" + repositoryId);
        return new DiversionRepository(response);
    }
    
    /**
     * Get file tree for a repository
     * Uses the /repos/{repo_id}/trees/{ref_id} endpoint
     */
    public List<DiversionFile> getFileTree(String repositoryId, String ref) throws IOException, InterruptedException {
        String accessToken = getAccessToken();
        String treesUrl = API_BASE_URL + "/repos/" + repositoryId + "/trees/" + ref;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(treesUrl))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get file tree: " + response.statusCode() + " - " + response.body());
        }
        
        // Parse the tree response
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        JsonNode itemsNode = root.get("items");
        
        List<DiversionFile> files = new ArrayList<>();
        
        if (itemsNode != null && itemsNode.isArray()) {
            for (JsonNode entry : itemsNode) {
                DiversionFile file = new DiversionFile();
                file.setPath(entry.get("path").asText());
                
                // Determine type based on blob presence
                JsonNode blobNode = entry.get("blob");
                if (blobNode != null && !blobNode.isNull()) {
                    file.setType("blob"); // It's a file
                } else {
                    file.setType("tree"); // It's a directory
                }
                
                files.add(file);
            }
        }
        
        return files;
    }
    
    /**
     * Get file content by path and ref
     */
    public String getFileContent(String repositoryId, String ref, String filePath) throws IOException, InterruptedException {
        // URL encode the file path
        String encodedFilePath = java.net.URLEncoder.encode(filePath, "UTF-8");
        
        // Make request to blob endpoint
        String accessToken = getAccessToken();
        String blobUrl = API_BASE_URL + "/repos/" + repositoryId + "/blobs/" + ref + "/" + encodedFilePath;
        
        HttpRequest blobRequest = HttpRequest.newBuilder()
            .uri(URI.create(blobUrl))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
        
        HttpResponse<String> blobResponse = httpClient.send(blobRequest, HttpResponse.BodyHandlers.ofString());
        
        // Handle redirects (204 No Content or 302 redirect with Location header)
        if (blobResponse.statusCode() == 204 || blobResponse.statusCode() == 302) {
            String locationHeader = blobResponse.headers().firstValue("Location").orElse(null);
            if (locationHeader != null) {
                // Follow the Location header to get the actual content
                HttpRequest contentRequest = HttpRequest.newBuilder()
                    .uri(URI.create(locationHeader))
                    .GET()
                    .build();
                
                HttpResponse<String> contentResponse = httpClient.send(contentRequest, HttpResponse.BodyHandlers.ofString());
                
                if (contentResponse.statusCode() >= 400) {
                    throw new IOException("Failed to get file content from Location URL: " + contentResponse.statusCode() + " - " + contentResponse.body());
                }
                
                return contentResponse.body();
            } else {
                throw new IOException("Blob endpoint returned redirect but no Location header found");
            }
        } else if (blobResponse.statusCode() >= 400) {
            throw new IOException("Failed to get file content: " + blobResponse.statusCode() + " - " + blobResponse.body());
        }
        
        return blobResponse.body();
    }
    
    /**
     * List all repositories accessible to the user
     * Useful for UI dropdown selection
     */
    public List<DiversionRepository> listRepositories() throws IOException, InterruptedException {
        JsonNode response = makeRequest("/repos");
        List<DiversionRepository> repositories = new ArrayList<>();
        
        if (response.has("items") && response.get("items").isArray()) {
            for (JsonNode repoNode : response.get("items")) {
                repositories.add(new DiversionRepository(repoNode));
            }
        } else if (response.isArray()) {
            // Fallback if API returns array directly
            for (JsonNode repoNode : response) {
                repositories.add(new DiversionRepository(repoNode));
            }
        }
        
        return repositories;
    }
    
    /**
     * List all branches for a repository
     */
    public List<DiversionBranch> listBranches(String repositoryId) throws IOException, InterruptedException {
        JsonNode response = makeRequest("/repos/" + repositoryId + "/branches");
        List<DiversionBranch> branches = new ArrayList<>();
        
        if (response.has("items") && response.get("items").isArray()) {
            for (JsonNode branchNode : response.get("items")) {
                branches.add(new DiversionBranch(branchNode));
            }
        } else if (response.isArray()) {
            // Fallback if API returns array directly
            for (JsonNode branchNode : response) {
                branches.add(new DiversionBranch(branchNode));
            }
        }
        
        return branches;
    }
    
    /**
     * Get branch details by ID or name
     */
    public DiversionBranch getBranchDetails(String repositoryId, String branchIdOrName) throws IOException, InterruptedException {
        // If it looks like a branch ID, use it directly
        if (branchIdOrName.startsWith("dv.branch.")) {
            JsonNode response = makeRequest("/repos/" + repositoryId + "/branches/" + branchIdOrName);
            return new DiversionBranch(response);
        }
        
        // Otherwise, search for the branch by name
        List<DiversionBranch> branches = listBranches(repositoryId);
        for (DiversionBranch branch : branches) {
            if (branch.getName().equals(branchIdOrName)) {
                return branch;
            }
        }
        
        throw new IOException("Branch not found: " + branchIdOrName);
    }
    
    /**
     * Resolve branch name to ID
     * Returns branch ID if already an ID, otherwise looks up by name
     */
    public String resolveBranchId(String repositoryId, String branchNameOrId) throws IOException, InterruptedException {
        if (branchNameOrId.startsWith("dv.branch.")) {
            return branchNameOrId;
        }
        
        DiversionBranch branch = getBranchDetails(repositoryId, branchNameOrId);
        return branch.getId();
    }
    
    /**
     * List commits for a repository
     */
    public List<DiversionCommit> listCommits(String repositoryId, int limit) throws IOException, InterruptedException {
        JsonNode response = makeRequest("/repos/" + repositoryId + "/commits?limit=" + limit);
        List<DiversionCommit> commits = new ArrayList<>();
        
        if (response.has("items") && response.get("items").isArray()) {
            for (JsonNode commitNode : response.get("items")) {
                commits.add(new DiversionCommit(commitNode));
            }
        } else if (response.isArray()) {
            // Fallback if API returns array directly
            for (JsonNode commitNode : response) {
                commits.add(new DiversionCommit(commitNode));
            }
        }
        
        return commits;
    }
    
    /**
     * Get the latest commit for a specific branch
     * Uses the branch's current commit ID to get the actual commit details
     */
    public DiversionCommit getLatestCommit(String repositoryId, String branchId) throws IOException, InterruptedException {
        // Get branch details to find the current commit ID
        DiversionBranch branch = getBranchDetails(repositoryId, branchId);
        String commitId = branch.getCommitId();
        
        if (commitId == null || commitId.isEmpty()) {
            // Fallback: try to get the first commit from the list
            List<DiversionCommit> commits = listCommits(repositoryId, 1);
            if (commits.isEmpty()) {
                throw new IOException("No commits found for repository: " + repositoryId);
            }
            return commits.get(0);
        }
        
        // Get the actual commit details using the commit ID
        return getCommitDetails(repositoryId, commitId);
    }
    
    /**
     * Get detailed commit information including changed files
     */
    public DiversionCommit getCommitDetails(String repositoryId, String commitId) throws IOException, InterruptedException {
        JsonNode response = makeRequest("/repos/" + repositoryId + "/commits/" + commitId);
        return new DiversionCommit(response);
    }
    
    /**
     * List all tags for a repository
     */
    public List<DiversionTag> listTags(String repositoryId) throws IOException, InterruptedException {
        String url = API_BASE_URL + "/repos/" + repositoryId + "/tags";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + getAccessToken())
            .header("Content-Type", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IOException("Failed to list tags: " + response.statusCode() + " - " + response.body());
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        
        List<DiversionTag> tags = new ArrayList<>();
        if (root.has("items") && root.get("items").isArray()) {
            for (JsonNode tagNode : root.get("items")) {
                tags.add(new DiversionTag(tagNode));
            }
        }
        
        return tags;
    }
    
    /**
     * Get a specific tag by ID
     */
    public DiversionTag getTag(String repositoryId, String tagId) throws IOException, InterruptedException {
        String url = API_BASE_URL + "/repos/" + repositoryId + "/tags/" + tagId;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + getAccessToken())
            .header("Content-Type", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IOException("Failed to get tag: " + response.statusCode() + " - " + response.body());
        }
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        
        return new DiversionTag(root);
    }
    
    /**
     * Create a new tag
     */
    public DiversionTag createTag(String repositoryId, String tagName, String commitId, String description) throws IOException, InterruptedException {
        String url = API_BASE_URL + "/repos/" + repositoryId + "/tags";
        
        // Create the request body
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("name", tagName);
        requestBody.put("commit_id", commitId);
        if (description != null && !description.isEmpty()) {
            requestBody.put("description", description);
        }
        
        String requestBodyJson = mapper.writeValueAsString(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + getAccessToken())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IOException("Failed to create tag: " + response.statusCode() + " - " + response.body());
        }
        
        JsonNode root = mapper.readTree(response.body());
        return new DiversionTag(root);
    }
    
    /**
     * Delete a tag
     */
    public void deleteTag(String repositoryId, String tagId) throws IOException, InterruptedException {
        String url = API_BASE_URL + "/repos/" + repositoryId + "/tags/" + tagId;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + getAccessToken())
            .header("Content-Type", "application/json")
            .DELETE()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 400) {
            throw new IOException("Failed to delete tag: " + response.statusCode() + " - " + response.body());
        }
    }
}
