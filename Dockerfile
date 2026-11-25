# HSRWiki backend image (used by Render; works on any Docker host).
# Stage 1 builds the jar with Maven, stage 2 only carries a Java runtime and the jar.
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first so they're cached between builds when only the code changes
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Free hosting gives 512 MB: keep the JVM well inside it (heap ~60%, small GC and code cache).
# TieredStopAtLevel=1 uses only the quick JIT compiler: faster start-up, plenty for this app.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=128m -Xss512k -XX:TieredStopAtLevel=1"

# Faster start-up, which matters because Render's free plan starts the app again every time it wakes up:
# 1. Unpack the jar (Java reads loose jars faster than jars nested inside one)
RUN java -Djarmode=tools -jar app.jar extract --destination extracted && rm app.jar
WORKDIR /app/extracted
# 2. Training run: start the app once and save the classes it loaded, already parsed, in a CDS archive.
#    spring.context.exit=onRefresh stops it as soon as everything is loaded.
#    If it fails, the image still works: Java just starts without the archive.
RUN java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar app.jar \
    || echo "No CDS archive: the app will start without it"

# Run as an unprivileged user, not root: the app only reads its files
RUN useradd --system --no-create-home --shell /usr/sbin/nologin app
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-jar", "app.jar"]
