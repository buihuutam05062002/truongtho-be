# ---- Giai đoạn 1: build file jar ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /app/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT:-8080} -jar app.jar"]