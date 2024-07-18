echo "Building Terpal Locally"
./gradlew :terpal-plugin-kotlin:build :terpal-plugin-gradle:build :terpal-runtime:build -Pnosign
