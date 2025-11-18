#!/bin/bash
# Switch back to personal ibain profile

cd /Users/ianbain/Diversion/Impostors_Redux/Meta/Jenkins/diversion-scm-plugin

echo "Removing local git config..."
git config --unset user.name 2>/dev/null
git config --unset user.email 2>/dev/null

echo ""
echo "✅ Switched back to global config (personal account):"
echo "  Name: $(git config --global user.name)"
echo "  Email: $(git config --global user.email)"
