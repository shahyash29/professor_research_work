FROM eclipse-temurin:23-jdk
WORKDIR /app
COPY spring-boot-server /app
WORKDIR /app
RUN apt-get update && \
    apt-get install -y maven && \
    mvn clean package -DskipTests
EXPOSE 8080

CMD ["java", "-jar", "target/trafficcsv-0.0.1-SNAPSHOT.jar"]
