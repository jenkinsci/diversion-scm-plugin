package io.superstudios.plugins.diversion;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a file or directory in a Diversion repository.
 * Used by Jenkins Global Libraries to navigate and read library files.
 */
public class DiversionSCMFile extends SCMFile {
    
    private final DiversionSCMFileSystem fileSystem;
    private final DiversionApiClient apiClient;
    private final String repositoryId;
    private final String branchId;
    private final String path;
    private Boolean isDirectory;
    private Boolean exists;
    
    protected DiversionSCMFile(@NonNull DiversionSCMFileSystem fileSystem, @NonNull String path,
                               DiversionApiClient apiClient, String repositoryId, String branchId) {
        super();
        this.fileSystem = fileSystem;
        this.path = path;
        this.apiClient = apiClient;
        this.repositoryId = repositoryId;
        this.branchId = branchId;
    }
    
    private DiversionSCMFile(@NonNull DiversionSCMFile parent, @NonNull String name) {
        super(parent, name);
        this.fileSystem = parent.fileSystem;
        this.apiClient = parent.apiClient;
        this.repositoryId = parent.repositoryId;
        this.branchId = parent.branchId;
        this.path = parent.path.isEmpty() ? name : parent.path + "/" + name;
    }
    
    @Override
    @NonNull
    protected SCMFile newChild(@NonNull String name, boolean assumeIsDirectory) {
        return new DiversionSCMFile(this, name);
    }
    
    @Override
    @NonNull
    public Iterable<SCMFile> children() throws IOException, InterruptedException {
        List<SCMFile> children = new ArrayList<>();
        
        // Get file tree from Diversion API
        List<DiversionFile> files = apiClient.getFileTree(repositoryId, branchId);
        
        // Normalize path for comparison
        String searchPath = path.isEmpty() ? "" : (path.endsWith("/") ? path : path + "/");
        
        // Track both direct files and subdirectories
        java.util.Map<String, Boolean> childInfo = new java.util.LinkedHashMap<>();
        
        // First pass: identify all children and whether they're directories
        for (DiversionFile file : files) {
            String filePath = file.getPath();
            
            if (filePath != null && filePath.startsWith(searchPath)) {
                // Get the relative path from this directory
                String relativePath = filePath.substring(searchPath.length());
                
                if (!relativePath.isEmpty()) {
                    // Check if this is a direct child or a nested path
                    int slashIndex = relativePath.indexOf('/');
                    
                    if (slashIndex == -1) {
                        // Direct child - could be file or directory
                        // We'll determine this by checking if other files have this as a prefix
                        String childName = relativePath;
                        
                        // Check if any other file starts with this path + "/"
                        String possibleDirPrefix = searchPath + childName + "/";
                        boolean hasChildren = false;
                        for (DiversionFile otherFile : files) {
                            if (otherFile.getPath() != null && otherFile.getPath().startsWith(possibleDirPrefix)) {
                                hasChildren = true;
                                break;
                            }
                        }
                        
                        // Only add if not already marked as directory
                        if (!childInfo.containsKey(childName) || !childInfo.get(childName)) {
                            childInfo.put(childName, hasChildren);
                        }
                    } else {
                        // Nested path - the first component is definitely a directory
                        String dirName = relativePath.substring(0, slashIndex);
                        childInfo.put(dirName, true);
                    }
                }
            }
        }
        
        // Second pass: create child objects
        for (java.util.Map.Entry<String, Boolean> entry : childInfo.entrySet()) {
            String childName = entry.getKey();
            boolean isDir = entry.getValue();
            
            DiversionSCMFile child = new DiversionSCMFile(this, childName);
            child.isDirectory = isDir;
            child.exists = true;
            children.add(child);
        }
        
        return children;
    }
    
    @Override
    public long lastModified() throws IOException, InterruptedException {
        // Could be enhanced to return actual file modification time
        return 0;
    }
    
    @Override
    @NonNull
    protected Type type() throws IOException, InterruptedException {
        if (isDirectory != null) {
            return isDirectory ? Type.DIRECTORY : Type.REGULAR_FILE;
        }
        
        // Determine if this is a file or directory by checking the file tree
        try {
            // If path is empty or is a known directory structure, it's a directory
            if (path.isEmpty() || path.equals("vars") || path.equals("src") || path.equals("resources")) {
                isDirectory = true;
                exists = true;
                return Type.DIRECTORY;
            }
            
            // Check if this path exists in the file tree
            List<DiversionFile> files = apiClient.getFileTree(repositoryId, branchId);
            
            // First check if any files start with this path (it's a directory)
            String searchPrefix = path + "/";
            boolean hasChildren = false;
            for (DiversionFile file : files) {
                if (file.getPath() != null && file.getPath().startsWith(searchPrefix)) {
                    hasChildren = true;
                    break;
                }
            }
            
            if (hasChildren) {
                isDirectory = true;
                exists = true;
                return Type.DIRECTORY;
            }
            
            // Check if this exact path exists as a file
            for (DiversionFile file : files) {
                if (path.equals(file.getPath())) {
                    isDirectory = false;
                    exists = true;
                    return Type.REGULAR_FILE;
                }
            }
            
            // Doesn't exist
            exists = false;
            return Type.NONEXISTENT;
            
        } catch (Exception e) {
            exists = false;
            return Type.NONEXISTENT;
        }
    }
    
    @Override
    @NonNull
    public InputStream content() throws IOException, InterruptedException {
        // Get file content from Diversion API
        String fileContent = apiClient.getFileContent(repositoryId, branchId, path);
        return new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    @NonNull
    public String getPath() {
        return path;
    }
}
