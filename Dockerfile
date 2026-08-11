# dev環境のECS Fargate向け本番イメージ（#00044）。ビルドと実行でJDK/JREを分けてイメージを小さくする。
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
