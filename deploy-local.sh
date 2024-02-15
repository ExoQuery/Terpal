echo "Deploying Terpal Locally"
./gradlew :terpal-runtime:publishToMavenLocal :terpal-plugin-kotlin:publishToMavenLocal && ./gradlew :terpal-plugin-gradle:publishToMavenLocal -Pnosign
