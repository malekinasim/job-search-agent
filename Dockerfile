# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Maven wrapper + root pom first (better layer caching for deps)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Everything else — .dockerignore keeps secrets/build junk out
COPY . .

RUN ./mvnw -B -pl job-search-agent -am clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app

COPY --from=build /app/job-search-agent/target/job-search-agent.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]