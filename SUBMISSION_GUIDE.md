# Jenkins Plugin Submission Guide

Complete step-by-step guide to submit the Diversion SCM Plugin to the Jenkins plugin repository.

## Prerequisites Checklist

Before submitting, ensure you have:

- [x] Plugin builds successfully (`mvn clean package`)
- [x] No compilation errors
- [x] README.md with comprehensive documentation
- [x] LICENSE file (MIT)
- [x] CHANGELOG.md documenting changes
- [x] Version is not SNAPSHOT (currently: 1.0.0)
- [ ] GitHub repository created and code pushed
- [ ] GitHub repository URL updated in pom.xml

## Step 1: Prepare GitHub Repository

### 1.1 Create GitHub Repository

1. Go to https://github.com/new
2. Create a new repository:
   - **Repository name**: `jenkins-diversion-scm-plugin` (or your preferred name)
   - **Description**: `Jenkins SCM plugin for Diversion repositories`
   - **Visibility**: Public (required for Jenkins hosting)
   - **License**: MIT License
   - **DO NOT** initialize with README (you already have one)

### 1.2 Push Your Code

```bash
cd /Users/ianbain/Diversion/Impostors_Redux/Meta/Jenkins/diversion-scm-plugin

# Initialize git if not already done
git init

# Add all files
git add .

# Create initial commit
git commit -m "Initial release: Diversion SCM Plugin v1.0.0"

# Add your GitHub repository as remote (replace YOUR_USERNAME)
git remote add origin https://github.com/YOUR_USERNAME/jenkins-diversion-scm-plugin.git

# Push to GitHub
git branch -M main
git push -u origin main
```

### 1.3 Update pom.xml with GitHub URL

**IMPORTANT**: Update the `<url>` and `<scm>` sections in `pom.xml` with your actual GitHub repository URL:

```xml
<url>https://github.com/YOUR_USERNAME/jenkins-diversion-scm-plugin</url>
<scm>
  <connection>scm:git:https://github.com/YOUR_USERNAME/jenkins-diversion-scm-plugin.git</connection>
  <developerConnection>scm:git:ssh://git@github.com:YOUR_USERNAME/jenkins-diversion-scm-plugin.git</developerConnection>
  <url>https://github.com/YOUR_USERNAME/jenkins-diversion-scm-plugin</url>
</scm>
```

Replace `YOUR_USERNAME` with your actual GitHub username.

## Step 2: Build Release Version

### 2.1 Final Build

```bash
cd /Users/ianbain/Diversion/Impostors_Redux/Meta/Jenkins/diversion-scm-plugin

# Clean and build
mvn clean package -DskipTests

# Verify the HPI file was created
ls -lh target/diversion-scm-1.0.0.hpi
```

The file `target/diversion-scm-1.0.0.hpi` is your plugin package.

### 2.2 Test the Plugin Locally (Optional but Recommended)

```bash
# Run Jenkins with your plugin
mvn hpi:run

# Jenkins will start at http://localhost:8080/jenkins
# Test all functionality before submitting
```

## Step 3: Request Jenkins Hosting

### 3.1 Create Hosting Request

1. **Go to**: https://github.com/jenkins-infra/repository-permissions-updater
2. **Sign in** with your GitHub account
3. **Create a new issue** using this template: https://github.com/jenkins-infra/repository-permissions-updater/issues/new

### 3.2 Fill Out the Issue Template

Use this information to fill out the template:

**Plugin Information:**
- **Plugin name**: `diversion-scm`
- **Plugin ID**: `diversion-scm`
- **GitHub repository**: `https://github.com/YOUR_USERNAME/jenkins-diversion-scm-plugin`
- **Plugin description**: `Jenkins SCM plugin for Diversion repositories - provides pipeline checkout and Global Pipeline Library support`
- **Maintainer GitHub username**: `YOUR_GITHUB_USERNAME`
- **Maintainer email**: `ian@superstudios.io`

**Additional Information:**
- **License**: MIT
- **Minimum Jenkins version**: 2.401.3
- **Java version**: 11

### 3.3 Issue Template Content

Copy and paste this into the GitHub issue (replace placeholders):

```markdown
## Plugin Information

- **Plugin name**: diversion-scm
- **Plugin ID**: diversion-scm
- **GitHub repository**: https://github.com/YOUR_USERNAME/jenkins-diversion-scm-plugin
- **Plugin description**: Jenkins SCM plugin for Diversion repositories - provides pipeline checkout and Global Pipeline Library support
- **Maintainer GitHub username**: YOUR_GITHUB_USERNAME
- **Maintainer email**: ian@superstudios.io

## Repository Details

- **License**: MIT
- **Minimum Jenkins version**: 2.401.3
- **Java version**: 11

## Additional Information

This plugin integrates Jenkins with Diversion (https://diversion.dev) repositories, providing:
- Pipeline job checkout (Legacy SCM)
- Global Pipeline Library support (Modern SCM)
- Full changelog display with commit IDs
- Interactive UI with dropdown menus

The plugin is ready for submission and has been tested in production.
```

## Step 4: Wait for Approval

After submitting the issue:

1. **Jenkins Infrastructure Team** will review your request
2. They may ask questions or request changes
3. Once approved, they will:
   - Fork your repository to `jenkinsci/jenkins-diversion-scm-plugin`
   - Grant you admin access
   - Set up CI/CD builds

**Typical wait time**: 1-2 weeks (can vary)

## Step 5: After Approval

### 5.1 Repository Forked

Once approved, your repository will be at:
- `https://github.com/jenkinsci/jenkins-diversion-scm-plugin`

### 5.2 Update Your Local Repository

```bash
# Add the jenkinsci fork as upstream
git remote add upstream https://github.com/jenkinsci/jenkins-diversion-scm-plugin.git

# Fetch from upstream
git fetch upstream

# Your original repo remains as 'origin'
```

### 5.3 Set Up CI/CD

1. Create a `Jenkinsfile` in your repository root (see below)
2. The Jenkins infrastructure team will set up CI builds automatically

## Step 6: Create Jenkinsfile for CI

Create `Jenkinsfile` in the repository root:

```groovy
buildPlugin(
    platforms: ['linux'],
    jdkVersions: [11],
    useContainerAgent: true
)
```

## Step 7: Release Process

### 7.1 Tag Release

```bash
# Tag the release
git tag -a diversion-scm-1.0.0 -m "Release version 1.0.0"
git push origin diversion-scm-1.0.0
git push upstream diversion-scm-1.0.0
```

### 7.2 Upload to Plugin Repository

After CI builds succeed, the plugin will be automatically:
- Built and tested
- Uploaded to the Jenkins plugin repository
- Made available in the Jenkins Update Center

## Important Notes

### Before Submission

1. **Update pom.xml URLs**: Replace `your-username` with your actual GitHub username
2. **Test thoroughly**: Make sure everything works in a clean Jenkins instance
3. **Documentation**: Ensure README.md is comprehensive
4. **License**: MIT license file must be present

### After Submission

1. **Monitor the issue**: Respond promptly to any questions
2. **Be patient**: The review process can take time
3. **Follow up**: If no response after 2 weeks, politely follow up

### Plugin Repository URL

Once published, your plugin will be available at:
- **Plugin Index**: https://plugins.jenkins.io/diversion-scm/
- **Update Center**: Available in Jenkins → Manage Plugins → Available

## Checklist Summary

- [ ] GitHub repository created and public
- [ ] Code pushed to GitHub
- [ ] pom.xml updated with correct GitHub URL
- [ ] LICENSE file present (MIT)
- [ ] README.md complete
- [ ] CHANGELOG.md present
- [ ] Version is 1.0.0 (not SNAPSHOT)
- [ ] Plugin builds successfully
- [ ] Plugin tested locally
- [ ] Hosting request issue created
- [ ] Jenkinsfile created (after approval)

## Useful Links

- **Jenkins Plugin Hosting Guide**: https://www.jenkins.io/doc/developer/publishing/requesting-hosting/
- **Plugin Development Guide**: https://www.jenkins.io/doc/developer/plugin-development/
- **Repository Permissions Updater**: https://github.com/jenkins-infra/repository-permissions-updater
- **Plugin Index**: https://plugins.jenkins.io/
- **Jenkins Developer Mailing List**: https://groups.google.com/g/jenkinsci-dev

## Support

If you encounter issues during submission:
- Check the [Jenkins Plugin Hosting FAQ](https://www.jenkins.io/doc/developer/publishing/requesting-hosting/)
- Ask on the [Jenkins Developer Mailing List](https://groups.google.com/g/jenkinsci-dev)
- Review similar plugin submissions for reference

Good luck with your submission! 🚀

