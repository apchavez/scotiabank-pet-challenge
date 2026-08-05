FROM eclipse-temurin:17-jdk@sha256:abb3826b404269a005829b63e2e7bd48a7be32115ab7ba9fa0d8cba834360eef AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew --version
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre@sha256:e4f018a55645ad204892e44eb35437518d7e108ba2a2dce305024ab371d24876
WORKDIR /app
RUN addgroup --system app && adduser --system --ingroup app app
COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
