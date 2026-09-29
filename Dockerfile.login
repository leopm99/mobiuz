# ---------------------------------------------------------------------------
# L2Journey Login Server - Multi-stage build from source
# ---------------------------------------------------------------------------
# Stage 1: Build with JDK 25 + Ant
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS builder

# Install Ant (Debian/Ubuntu)
RUN apt-get update && apt-get install -y --no-install-recommends ant && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Copy project (source + datapack)
COPY build.xml .
COPY java ./java
COPY dist ./dist

# Build jars only (compile + jar; do not run cleanup so we keep build/dist)
RUN ant compile jar

# ---------------------------------------------------------------------------
# Stage 2: Runtime with JRE only
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre

WORKDIR /opt/l2journey

# Layout: login/ (server dir) and libs/ (dependency jars). Script expects CWD=login and -cp "../libs/*:Loginserver.jar"
# Built jars are in build/build/dist/libs/ (Ant property build.dist.libs relative to /build)
COPY --from=builder /build/dist/login ./login
COPY --from=builder /build/dist/libs ./libs
COPY --from=builder /build/build/dist/libs/LoginServer.jar ./login/Loginserver.jar

# Login server port (client connections) and port for game server registration
EXPOSE 2106 9014

ENV L2J_LOGIN_OPTS="-Xms128m -Xmx256m"

CMD ["sh", "-c", "cd /opt/l2journey/login && exec java -server \
  -Dfile.encoding=UTF-8 \
  -Dorg.slf4j.simpleLogger.log.com.zaxxer.hikari=warn \
  -XX:+UseZGC \
  $L2J_LOGIN_OPTS \
  -Dlogback.configurationFile=./configuration/logback.xml \
  -cp \"./../libs/*:Loginserver.jar\" \
  com.l2journey.loginserver.LoginServer"]
