# Antes de construir a imagem: mvn -Pserver package
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY server/ /app/server/
ENV GASTOBOT_DB_PATH=/data/gastobot.db
ENTRYPOINT ["java", "-jar", "/app/server/mcp_server01_server.jar"]
