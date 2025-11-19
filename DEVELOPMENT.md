# Development Guide

This guide is for developers who want to build, test, or contribute to the Jenkins Diversion SCM Plugin.

## Prerequisites

- Jenkins 2.504.3 or later
- Java 17 or later
- Maven 3.9.6+ (required for Java 17 support)
- Diversion API refresh token ([Get one here](https://docs.diversion.dev/ci-cd))

## Installation

### Option 1: Build from Source (Recommended)

1. **Clone the plugin repository**:
   ```bash
   git clone https://github.com/ibain/jenkins-diversion-scm-plugin.git
   cd jenkins-diversion-scm-plugin
   ```

2. **Build the plugin**:
   ```bash
   ./build.sh
   # Or manually:
   mvn clean package -DskipTests
   ```
   
   This creates the HPI file at `target/diversion-scm-1.0.1-SNAPSHOT.hpi`

3. **Install the plugin**:
   - Go to Jenkins → Manage Jenkins → Manage Plugins
   - Click "Advanced" tab
   - Upload the generated `.hpi` file

### Option 2: Deploy to Kubernetes Jenkins

If you're running Jenkins in Kubernetes:

```bash
# Build the plugin
cd jenkins-diversion-scm-plugin
mvn clean package -DskipTests

# Deploy to Jenkins pod
kubectl cp target/diversion-scm-1.0.1-SNAPSHOT.hpi default/jenkins-0:/var/jenkins_home/plugins/diversion-scm.jpi

# Restart Jenkins
kubectl delete pod jenkins-0
```

The plugin will be automatically loaded on Jenkins restart.

### Option 3: Development Mode

Run Jenkins with the plugin in development mode:

```bash
mvn hpi:run
```

This starts Jenkins at `http://localhost:8080/jenkins` with the plugin loaded.

## Building

### Standard Build

```bash
mvn clean package
```

### Build Without Tests

```bash
mvn clean package -DskipTests
```

### Run Tests

```bash
mvn test
```

### Full Verification

```bash
mvn clean verify
```

This runs all tests, SpotBugs static analysis, and other checks.

## Debugging

### Debug Scripts

Debug scripts can be created as needed for troubleshooting. Run any Groovy scripts in Jenkins Script Console (Manage Jenkins → Script Console).

### Debug Logging

Enable debug logging for the plugin:

1. Go to **Manage Jenkins** → **System Log**
2. Add new log recorder for `io.superstudios.plugins.diversion`
3. Set log level to `FINE` or `FINEST`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Run `mvn clean verify` to ensure all checks pass
6. Submit a pull request

## Project Structure

```
diversion-scm-plugin/
├── src/
│   ├── main/
│   │   ├── java/io/superstudios/plugins/diversion/
│   │   │   ├── DiversionSCM.java              # Legacy SCM implementation
│   │   │   ├── DiversionSCMSource.java        # Modern SCM (Global Libraries)
│   │   │   ├── DiversionApiClient.java        # API client with proxy support
│   │   │   ├── DiversionUIHelper.java         # Shared UI helper methods
│   │   │   └── ...                            # Other classes
│   │   └── resources/                         # Jelly templates
│   └── test/                                  # Test files
├── pom.xml                                    # Maven configuration
├── Jenkinsfile                                # CI/CD pipeline
└── README.md                                  # User-facing documentation
```

## Code Style

- Follow Java coding conventions
- Use 4 spaces for indentation
- Add Javadoc comments for public methods
- Run SpotBugs checks: `mvn spotbugs:check`
- Ensure all tests pass before submitting PRs

## Security

The plugin follows Jenkins security best practices:
- All web methods have permission checks
- CSRF protection via `@RequirePOST` annotations
- Credential enumeration protection
- Proxy support with authentication

See the main [README.md](README.md) for user-facing documentation.

