package io.superstudios.plugins.diversion;

import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Change log parser for Diversion commits.
 * Extends ChangeLogParser to integrate with Jenkins SCM.
 */
public class DiversionChangeLogParser extends hudson.scm.ChangeLogParser {
    
    @Override
    public ChangeLogSet<? extends ChangeLogSet.Entry> parse(Run build, 
                                                           RepositoryBrowser browser,
                                                           File changelogFile) 
                                                           throws IOException {
        
        List<DiversionChangeLogEntry> entries = new ArrayList<>();
        
        // If no changelog file exists, return empty set
        if (changelogFile == null || !changelogFile.exists()) {
            return new DiversionChangeLogSet(build, entries);
        }
        
        try {
            // Parse the XML changelog file
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(changelogFile);
            
            org.w3c.dom.NodeList entryNodes = doc.getElementsByTagName("entry");
            
            for (int i = 0; i < entryNodes.getLength(); i++) {
                org.w3c.dom.Element entryElement = (org.w3c.dom.Element) entryNodes.item(i);
                
                String commitId = getElementText(entryElement, "commitId");
                String msg = getElementText(entryElement, "msg");
                String authorName = getElementText(entryElement, "author");
                String timestampStr = getElementText(entryElement, "timestamp");
                
                long timestamp = 0;
                try {
                    timestamp = Long.parseLong(timestampStr);
                } catch (NumberFormatException e) {
                    // Use current time if parsing fails
                    timestamp = System.currentTimeMillis() / 1000;
                }
                
                // Parse changed files if available
                java.util.Collection<String> affectedPaths = new java.util.ArrayList<>();
                org.w3c.dom.NodeList fileNodes = entryElement.getElementsByTagName("file");
                for (int j = 0; j < fileNodes.getLength(); j++) {
                    String filePath = fileNodes.item(j).getTextContent();
                    if (filePath != null && !filePath.trim().isEmpty()) {
                        affectedPaths.add(filePath.trim());
                    }
                }
                
                // Create a simple author object
                DiversionAuthor author = new DiversionAuthor();
                author.setName(authorName);
                author.setEmail("");
                author.setFullName(authorName);
                author.setId("");
                
                DiversionChangeLogEntry entry = new DiversionChangeLogEntry(
                    commitId,
                    msg,
                    author,
                    timestamp,
                    affectedPaths
                );
                entries.add(entry);
            }
            
            // Create the change log set and set parent for all entries
            DiversionChangeLogSet changeLogSet = new DiversionChangeLogSet(build, entries);
            // Set parent for each entry after the set is created
            for (DiversionChangeLogEntry entry : entries) {
                entry.setParentSet(changeLogSet);
            }
            
            return changeLogSet;
        } catch (Exception e) {
            // If we can't parse the changelog file, return empty set
            // This prevents the build from failing
            System.err.println("Error parsing changelog: " + e.getMessage());
            e.printStackTrace();
        }
        
        return new DiversionChangeLogSet(build, entries);
    }
    
    /**
     * Helper method to get text content from XML element
     */
    private String getElementText(org.w3c.dom.Element parent, String tagName) {
        org.w3c.dom.NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }
}