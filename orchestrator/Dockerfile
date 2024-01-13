FROM  maven:3.9.1-eclipse-temurin-20-alpine
COPY . /usr/src/myapp
WORKDIR /usr/src/myapp
RUN mvn package -DskipTests