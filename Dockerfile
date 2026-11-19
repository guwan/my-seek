FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x ./gradlew

COPY src ./src
RUN ./gradlew bootJar --no-daemon --console=plain

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system myseek && useradd --system --gid myseek --home-dir /app myseek \
    && mkdir -p /app/uploads/documents \
    && chown -R myseek:myseek /app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

USER myseek
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
