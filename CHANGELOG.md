# Changelog

All notable changes to the Jenkins Diversion SCM Plugin will be documented in this file.

## [1.0.1] - 2025-11-19

### Fixed
- **Folder-scoped credentials**: Fixed credential lookup during build execution to properly resolve folder-scoped credentials using `findCredentialById()` with Run context
- **Credential tracking**: Added `CredentialsProvider.track()` calls to enable credential usage reporting in Jenkins

### Improved
- **Modern Credentials API**: Updated to use modern Credentials API pattern (passing `null` for Authentication, letting Jenkins resolve security internally)
- **UI improvements**: Changed to `StandardListBoxModel` from CredentialsPlugin for proper credential display formatting
- **Proxy support**: Enhanced proxy configuration to use `ProxyConfiguration.newHttpClient()` which handles authentication automatically
- **Permission checks**: Simplified permission checks to only verify `USE_ITEM` (EXTENDED_READ implies it)
- **Better UX**: Changed from `checkPermission()` to `hasPermission()` for better user experience (returns empty list instead of error)
- **Relaxed permissions**: Using `Jenkins.MANAGE` instead of `ADMINISTER` for more relaxed permission requirements

### Security
- **CSRF protection**: Added `@RequirePOST` annotations to all external API calls
- **Credential enumeration protection**: Proper permission checks before accessing credentials
- **Explicit permission checks**: Using `checkPermission()` and `hasPermission()` for better CodeQL recognition

### Technical Details
- Updated `DiversionApiClient` to accept `Run` context and use `findCredentialById()` during execution
- Updated `DiversionSCM.checkout()` to track credential usage with `CredentialsProvider.track()`
- Updated `DiversionUIHelper` to use `StandardListBoxModel` for credential dropdowns
- Removed dependency on `ACL.SYSTEM`/`ACL.SYSTEM2` by using modern Credentials API pattern
- Fixed XML declaration order in `config.jelly` (must come before `<?jelly escape-by-default?>`)

## [1.0.0] - 2025-11-17

### Added
- Initial release of Diversion SCM Plugin
- Support for pipeline job checkout (Legacy SCM)
- Support for Global Pipeline Libraries (Modern SCM)
- Interactive UI with dropdown menus for credentials, repositories, and branches
- Changelog display with commit IDs, authors, messages, and changed files
- Smart library reloading based on commit timestamps
- Auto-detection of pipeline script files based on job name

### Fixed
- Changelog detail page now displays correctly with commit ID, author, date, and message
- Commit IDs now appear in the Changes page list view
- Proper parent relationship setup for changelog entries
- Global libraries now reload correctly when files change (uses commit timestamps)
- Removed stale workspaceId field from configuration

### Improved
- Code deduplication: Created `DiversionUIHelper` class for shared UI logic
- Enhanced error handling throughout the plugin
- Improved null safety checks
- Better Jelly template error handling
- Cleaned up unused imports and code

### Technical Details
- Fixed `DiversionChangeLogEntry` to properly set parent ChangeLogSet
- Added `getMsgAnnotated()` and `getDisplayName()` methods for Jenkins core template compatibility
- Updated Jelly templates to use `it.parent.run` instead of `it.parent.build`
- Enhanced `DiversionSCMFileSystem.lastModified()` to use actual commit timestamps
- Improved `DiversionApiClient.getLatestCommit()` to use branch commit IDs

## Future Improvements

Potential enhancements for future versions:
- Support for tags
- Workspace-specific checkout support
- Enhanced changelog filtering
- Performance optimizations for large repositories

