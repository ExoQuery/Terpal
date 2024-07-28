echo "Deploying Terpal Locally"
./gradlew :terpal-runtime:build :terpal-plugin-kotlin:build :terpal-plugin-gradle:build build -Pnosign
