sed -i 's/com.github.polymorphicshade:NewPipeExtractor:ac1c22d/com.github.TeamNewPipe:NewPipeExtractor:9d31e09745/g' app/build.gradle.kts
./gradlew :app:dependencies | grep NewPipeExtractor
