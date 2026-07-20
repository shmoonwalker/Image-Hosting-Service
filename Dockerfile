
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app


COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode dependency:go-offline


COPY src/ src/
RUN ./mvnw --batch-mode clean package -DskipTests

FROM eclipse-temurin:26-jre AS runtime
WORKDIR /app


COPY --from=build /app/target/*.jar app.jar

USER 10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
