# Jenkins Diversion SCM Plugin

A unified Jenkins SCM plugin that integrates with [Diversion](https://diversion.dev) repositories, providing both pipeline checkout and Global Pipeline Library support.

**Author:** Ian Bain (ibain@mac.com)  
**License:** MIT  
**Latest Version:** Available on [plugins.jenkins.io/diversion-scm](https://plugins.jenkins.io/diversion-scm/)

## Features

### Pipeline Job Checkout (Legacy SCM)
- **Repository Checkout**: Checkout files from Diversion repositories for pipeline jobs
- **Branch Support**: Support for different branches and refs
- **Credential Integration**: Secure API token management through Jenkins credentials
- **Pipeline Support**: Works with both freestyle and pipeline jobs
- **Auto-Detection**: Automatically detects pipeline script files based on job name
- **Changelog Support**: Full changelog display with commit IDs, authors, messages, and changed files

### Global Pipeline Libraries (Modern SCM)
- **Library Loading**: Load shared libraries from Diversion repositories
- **Interactive UI**: Dropdown menus for repository and branch selection
- **Auto-Discovery**: Automatically discovers available repositories and branches
- **Library Path Configuration**: Configurable base path for library files (e.g., `Meta/Jenkins/SharedLib`)
- **Smart Reloading**: Automatically detects when library files change based on commit timestamps
- **Seamless Integration**: Works alongside existing GitHub or Git-based libraries

### Changelog Display
- **Commit ID Display**: Shows commit IDs in both the Changes page list and detail pages
- **Detailed Views**: Full commit details including author, date, message, and changed files
- **Proper Navigation**: Working detail page links from the Changes page

### Security Features
- **Credential Protection**: Permission checks prevent unauthorized credential enumeration
- **CSRF Protection**: All external API calls are protected from cross-site request forgery attacks
- **Authorization**: Proper permission checks ensure only authorized users can access sensitive operations
- **Security Scanning**: Plugin passes Jenkins CodeQL security scans

## Prerequisites

- Jenkins 2.504.3 or later
- Java 17 or later
- Diversion API refresh token ([Get one here](https://docs.diversion.dev/ci-cd))

## Installation

The plugin can be installed from the [Jenkins Plugin Manager](https://plugins.jenkins.io/diversion-scm/) once published, or built from source.

**For developers:** See [DEVELOPMENT.md](DEVELOPMENT.md) for build instructions and development setup.

## Configuration

### 1. Add Diversion API Credentials

1. Go to **Manage Jenkins** → **Manage Credentials**
2. Click **Add Credentials**
3. Select **Secret text**
4. Fill in:
   - **ID**: `diversion-refresh-token` (or your preferred ID)
   - **Description**: `Diversion Refresh Token`
   - **Secret**: Your Diversion API refresh token

**Important:** You need a **refresh token**, not an access token. Get this from your [Diversion account settings](https://docs.diversion.dev/ci-cd).

### 2a. Configure SCM for Pipeline Jobs (Legacy SCM)

#### For Pipeline Jobs:

1. Create a new pipeline job
2. In **Pipeline** section, select **Pipeline script from SCM** under **Definition**
3. In **SCM**, select **Diversion**
4. Configure:
   - **Credentials**: Select your Diversion API token credential
   - **Repository**: Your Diversion repository (dropdown will populate after selecting credentials)
   - **Branch**: Branch to checkout (dropdown will populate after selecting repository)
   - **Script Path**: Optional path to specific .groovy file, or leave empty to auto-detect based on job name

The plugin will automatically search for a `.groovy` file matching your job name in common locations like `Meta/Jenkins/`, `Jenkins/`, `scripts/`, etc.

#### For Freestyle Jobs:

1. Create a new freestyle job
2. In **Source Code Management**, select **Diversion**
3. Configure the same fields as above

### 2b. Configure Global Pipeline Libraries (Modern SCM)

1. Go to **Manage Jenkins** → **System**
2. Scroll to **Global Trusted Pipeline Libraries**
3. Click **Add** to create a new library
4. Configure the library:
   - **Name**: `diversion-lib` (or your preferred name)
   - **Default Version**: `main` (or your preferred branch)
   - **Modern SCM**: Select **Diversion** from dropdown
   - **Credentials**: Select your Diversion API token credential
   - **Repository**: Your Diversion repository (dropdown populates automatically)
   - **Default Branch**: Branch to checkout (dropdown populates automatically)
   - **Library Base Path**: Path to your folder containing `vars/`, `src/`, `resources/` directories (e.g., `Meta/Jenkins/SharedLib`)

#### Important: Prevent Duplicate Commits in Changelog

**If your pipeline script and shared library are in the same Diversion repository**, you should **uncheck** the "Include @Library changes in job recent changes" option. Otherwise, Jenkins will show duplicate commit entries in the build status grid (one from the library checkout and one from the script checkout).

- In the library configuration, uncheck: **"Include @Library changes in job recent changes"**
- Or when configuring via Groovy script, add: `libraryConfig.setIncludeInChangesets(false)`

This is recommended whenever the library is stored in the same repository as your pipeline scripts to avoid confusing duplicate entries.

#### Using the Library in Pipeline Jobs:

```groovy
@Library('diversion-lib') _

pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                script {
                    // Use helpers from your library's vars/ directory
                    deploymentHelpers.deployToStaging()
                    
                    // Your build logic here
                }
            }
        }
    }
}
```

## Features in Detail

### Changelog Display

The plugin provides comprehensive changelog support:

- **Changes Page**: Shows commit IDs, messages, and authors in a numbered list
- **Detail Pages**: Click any commit to see full details including:
  - Commit ID
  - Author name and email
  - Commit date
  - Full commit message
  - List of changed files

### Smart Library Reloading

Global Pipeline Libraries automatically reload when:
- New commits are made to the repository
- Library files are updated
- The commit timestamp changes

The plugin checks the actual commit timestamp to determine if libraries need to be reloaded, ensuring you always have the latest version.

### Interactive UI

The plugin provides user-friendly dropdown menus that:
- Populate repositories based on selected credentials
- Populate branches based on selected repository
- Show repository and branch names with IDs in parentheses
- Validate configuration in real-time

## API Integration

This plugin integrates with the [Diversion API](https://docs.diversion.dev/api-reference/introduction) using:

- **Token Exchange**: `POST https://auth.diversion.dev/oauth2/token`
- **Repository List**: `GET https://api.diversion.dev/v0/repos`
- **Repository Details**: `GET https://api.diversion.dev/v0/repos/{repoId}`
- **Branch List**: `GET https://api.diversion.dev/v0/repos/{repoId}/branches`
- **File Tree**: `GET https://api.diversion.dev/v0/repos/{repoId}/trees/{refId}`
- **File Content**: `GET https://api.diversion.dev/v0/repos/{repoId}/blobs/{refId}/{path}`
- **Commits**: `GET https://api.diversion.dev/v0/repos/{repoId}/commits`

## Development

For build instructions, project structure, and contribution guidelines, see [DEVELOPMENT.md](DEVELOPMENT.md).

## Troubleshooting

### Common Issues

1. **Authentication Failed**
   - Verify your Diversion API refresh token is correct
   - Check that the Jenkins credential ID matches in job configuration
   - Ensure the token hasn't expired (refresh tokens don't expire, but check your Diversion account)

2. **Repository Not Found**
   - Verify the repository ID is correct
   - Check that your API token has access to the repository
   - Try refreshing the repository dropdown after selecting credentials

3. **File Checkout Failed**
   - Check network connectivity to Diversion API
   - Verify the branch/ref exists in the repository
   - Check Jenkins logs for detailed error messages

4. **Library Not Reloading**
   - The plugin checks commit timestamps to detect changes
   - If libraries aren't reloading, check that new commits are being made
   - Verify the library path is correct

5. **Changelog Not Showing**
   - Ensure the build has completed successfully
   - Check that commits exist in the repository
   - Verify the changelog XML file exists in the build directory

6. **Duplicate Commits in Build Status Grid**
   - This happens when your pipeline script and shared library are in the same repository
   - Jenkins creates changelog entries for both the script checkout and library checkout
   - **Fix**: In the library configuration, uncheck "Include @Library changes in job recent changes"
   - Or add `libraryConfig.setIncludeInChangesets(false)` when configuring via Groovy

## Changelog

For version history and recent improvements, see [CHANGELOG.md](CHANGELOG.md).

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This plugin is licensed under the MIT License.

## Support

For issues and questions:
- Create an issue in the repository
- Check the [Jenkins plugin documentation](https://www.jenkins.io/doc/developer/plugin-development/)
- Review the [Diversion API documentation](https://docs.diversion.dev/api-reference/introduction)

## Acknowledgments

Built with ❤️ for the Jenkins and Diversion communities.
