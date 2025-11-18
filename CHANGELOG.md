# Changelog

All notable changes to the Jenkins Diversion SCM Plugin will be documented in this file.

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

