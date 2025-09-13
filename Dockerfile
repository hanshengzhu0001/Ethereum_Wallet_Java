# Build stage
FROM maven:3.9-openjdk-17-slim AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim

# Install necessary packages for JavaFX (if needed)
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libgtk-3-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR file
COPY --from=build /app/target/ethereum-wallet-1.0.0.jar app.jar

# Create logs directory
RUN mkdir -p logs

# Expose the port
EXPOSE 8080

# Set JVM options
ENV JAVA_OPTS="-Xmx1g -Xms512m -Djava.awt.headless=true -Dethereum.wallet.mode=headless"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
