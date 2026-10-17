# --- Build ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml checkstyle.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# --- Runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 talentmatch
COPY --from=build /app/target/talentmatch-backend-*.jar app.jar
USER talentmatch
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
