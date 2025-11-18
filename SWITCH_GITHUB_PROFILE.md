# Switch GitHub Profile in CLI

## Quick Answer

You can switch GitHub profiles by changing git config. Here's how to do it temporarily and switch back.

## Check Current Profile

```bash
# Check global git config
git config --global user.name
git config --global user.email

# Check GitHub CLI (if installed)
gh auth status
```

## Option 1: Switch Per-Repository (Recommended)

This only affects the plugin repository, leaving your global config unchanged:

```bash
cd /Users/ianbain/Diversion/Impostors_Redux/Meta/Jenkins/diversion-scm-plugin

# Set for this repository only
git config user.name "ibain"
git config user.email "ian@superstudios.io"

# Verify
git config user.name
git config user.email
```

**To switch back later:**
```bash
cd /Users/ianbain/Diversion/Impostors_Redux/Meta/Jenkins/diversion-scm-plugin
git config --unset user.name
git config --unset user.email
```

## Option 2: Switch Globally Temporarily

If you need to switch globally:

```bash
# Save current config
OLD_NAME=$(git config --global user.name)
OLD_EMAIL=$(git config --global user.email)

# Switch to ibain profile
git config --global user.name "ibain"
git config --global user.email "ian@superstudios.io"

# Do your work...

# Switch back
git config --global user.name "$OLD_NAME"
git config --global user.email "$OLD_EMAIL"
```

## Option 3: Using GitHub CLI

If you use GitHub CLI (`gh`):

```bash
# Check current auth
gh auth status

# Switch account (will prompt for login)
gh auth login

# Switch back later
gh auth login --hostname github.com
```

## For This Plugin Push

Since you're pushing to `ibain/jenkins-diversion-scm-plugin`, you should use:

```bash
cd /Users/ianbain/Diversion/Impostors_Redux/Meta/Jenkins/diversion-scm-plugin

# Set for this repo only
git config user.name "ibain"
git config user.email "ian@superstudios.io"

# Then proceed with push
git init
git add .
git commit -m "Initial release: Diversion SCM Plugin v1.0.0"
git remote add origin https://github.com/ibain/jenkins-diversion-scm-plugin.git
git branch -M main
git push -u origin main
```

This way, your global config stays unchanged for other repositories.

