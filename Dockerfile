FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN apt-get update && apt-get install -y libgtk2.0-0 libavcodec-dev libavformat-dev libswscale-dev && rm -rf /var/lib/apt/lists/*
RUN ./mvnw clean package -DskipTests

# Mostrar el contenido de target
RUN ls -lah target

EXPOSE 8080

CMD ["sh", "-c", "ls -lah target && java -jar target/preguntas-0.0.1-SNAPSHOT.jar"]