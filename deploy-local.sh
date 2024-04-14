echo "Deploying Terpal Locally"
./gradlew :terpal-runtime:publishToMavenLocal :terpal-plugin-kotlin:publishToMavenLocal :terpal-plugin-gradle:publishToMavenLocal -Pnosign
