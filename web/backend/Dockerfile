FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY web/backend/build/libs/backend.jar app.jar
COPY web/api/openapi.yaml /app/web/api/openapi.yaml
EXPOSE 8080
ENV PORT=8080
CMD ["java", "-jar", "app.jar"]
