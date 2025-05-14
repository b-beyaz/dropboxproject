FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/dropboxproject-0.0.1-SNAPSHOT.war app.war

# Uploads dizinini oluştur ve izinleri ayarla
RUN mkdir -p /app/uploads && \
    chmod 777 /app/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
