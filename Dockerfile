FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x ./mvnw

# Copy source code
COPY src src

RUN ./mvnw clean package -DskipTests


CMD ["java", "-Dserver.port=${PORT:-8080}", "-jar", "target/power-city-platform-1.0.0.jar"]

EXPOSE ${PORT:-8080}