# Stop the script if any command fails
$ErrorActionPreference = "Stop"

Write-Host "build.ps1 running..."

# Run the Maven Wrapper command
./mvnw clean package -DskipTests -Pexclude-properties

# Copy the application.properties file into the target directory
Copy-Item -Path "src\main\resources\application.properties" -Destination "config\application.properties"

Write-Host "Build and copy operations completed!"