# Build: compila o JAR com o Maven Wrapper do projeto
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -q -B -DskipTests package && cp target/raizesdonordeste-*.jar app.jar

# Execução: JRE enxuto, usuário sem privilégios
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
RUN mkdir data && chown app:app data
USER app

# Perfil dev = H2 embutido + seed automático (ideal para demonstração).
# Defina JWT_SECRET no painel da hospedagem. A porta vem da variável PORT.
ENV SPRING_PROFILES_ACTIVE=dev \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
