#!/bin/bash
# Deploy the Diversion SCM plugin to Jenkins and restart

set -e

echo "Deploying Diversion SCM plugin to Jenkins..."

# Check if plugin file exists
if [ ! -f "target/diversion-scm-1.0.0.hpi" ]; then
    echo "❌ Plugin file not found. Run ./build.sh first."
    exit 1
fi

# Copy to Jenkins
echo "Copying plugin to Jenkins pod..."
kubectl cp target/diversion-scm-1.0.0.hpi default/jenkins-0:/var/jenkins_home/plugins/diversion-scm.jpi

# Restart Jenkins
echo "Restarting Jenkins..."
kubectl delete pod jenkins-0 -n default

echo ""
echo "⏳ Waiting for Jenkins to restart (60 seconds)..."
sleep 60

echo ""
echo "✅ Deployment complete!"
echo "Jenkins should be ready at your usual URL"


