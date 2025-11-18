# Push Plugin to GitHub

## Quick Answer

**No, it doesn't matter that you're pushing from within a Diversion repository!** 

As long as you:
1. Initialize git **only in the plugin directory** (not the parent)
2. Push **only the plugin code** (not the entire Diversion repo)
3. Push to the **separate GitHub repository** you created

You'll be fine! Git tracks what's in the repository you initialize, not where that directory is located on your filesystem.

## Step-by-Step Push Instructions

### Option 1: Initialize Git in Plugin Directory (Recommended)

```bash
cd /Users/ianbain/Diversion/Impostors_Redux/Meta/Jenkins/diversion-scm-plugin

# Initialize git repository (only in this directory)
git init

# Add all plugin files
git add .

# Create initial commit
git commit -m "Initial release: Diversion SCM Plugin v1.0.0"

# Add your GitHub repository as remote
git remote add origin https://github.com/ibain/jenkins-diversion-scm-plugin.git

# Push to GitHub
git branch -M main
git push -u origin main
```

### Option 2: If Parent Repo Already Has Git

If the parent Diversion repository already has git initialized, you can still push the plugin separately:

```bash
cd /Users/ianbain/Diversion/Impostors_Redux/Meta/Jenkins/diversion-scm-plugin

# Check if parent has git
cd .. && git status 2>&1 | head -1

# If parent has git, initialize a separate repo in plugin dir
cd diversion-scm-plugin
git init
git add .
git commit -m "Initial release: Diversion SCM Plugin v1.0.0"
git remote add origin https://github.com/ibain/jenkins-diversion-scm-plugin.git
git branch -M main
git push -u origin main
```

## What Gets Pushed

With the `.gitignore` file in place, only these files will be pushed:
- ✅ Source code (`src/`)
- ✅ Configuration files (`pom.xml`, `Jenkinsfile`)
- ✅ Documentation (`README.md`, `CHANGELOG.md`, `LICENSE`)
- ✅ Build scripts (`build.sh`, `deploy.sh`)
- ❌ Build artifacts (`target/` directory - ignored)
- ❌ IDE files (ignored)
- ❌ Temporary files (ignored)

## Verify Before Pushing

```bash
cd /Users/ianbain/Diversion/Impostors_Redux/Meta/Jenkins/diversion-scm-plugin

# See what will be committed
git status

# See what files will be pushed
git ls-files | head -20
```

You should see:
- `src/` directory files
- `pom.xml`
- `README.md`
- `LICENSE`
- `Jenkinsfile`
- etc.

You should **NOT** see:
- `target/` directory
- Parent repository files
- Diversion game files

## After Pushing

Once pushed, your GitHub repository at https://github.com/ibain/jenkins-diversion-scm-plugin will contain only the plugin code, completely independent of the Diversion repository structure.

## Next Steps

After pushing to GitHub:
1. Verify the repository looks correct on GitHub
2. Update the submission issue with the correct repository URL
3. Wait for Jenkins team approval

