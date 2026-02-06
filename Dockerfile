# Stage 1: Build the application using Gradle
FROM gradle:8.8.0-jdk21-jammy AS build

# Set the working directory
WORKDIR /home/gradle/src

# Copy the build.gradle, settings.gradle, and gradlew files
COPY build.gradle settings.gradle ./
COPY gradlew ./
COPY gradle ./gradle

# Download dependencies
RUN ./gradlew build -x test --parallel --build-cache || return 0

# Copy the rest of the source code
COPY src ./src

# Build the application jar
RUN ./gradlew bootJar

# Stage 2: Create the final, smaller image
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
