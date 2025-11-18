#!/bin/bash
# Quick build script for the Diversion SCM Jenkins plugin

set -e

echo "Building Diversion SCM plugin..."
mvn clean package -DskipTests

echo ""
echo "✅ Build complete!"
echo "Plugin location: target/diversion-scm-1.0.0.hpi"
echo ""
echo "To deploy to Jenkins:"
echo "  kubectl cp target/diversion-scm-1.0.0.hpi default/jenkins-0:/var/jenkins_home/plugins/diversion-scm.jpi"
echo "  kubectl delete pod jenkins-0"


