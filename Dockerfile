FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . /app
RUN apt-get-update && \
    apt-get install -y maven && \
    mvn clean package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/*.jar"]
