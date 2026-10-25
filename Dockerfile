# Imagen de un microservicio de DonaTrack. La misma receta sirve para los dos módulos:
#   docker build --build-arg MODULO=donaciones --build-arg MAIN_CLASS=... .
# El contexto es la raíz del repo porque cada módulo hereda del pom padre.

# ---------- Compilación ----------
FROM docker.io/library/maven:3.9.9-eclipse-temurin-17 AS build
ARG MODULO
WORKDIR /app

# El pom padre exige un JDK 17 declarado en toolchains.xml; adentro del contenedor es el de la imagen.
RUN mkdir -p /root/.m2 && printf '%s\n' \
    '<toolchains><toolchain><type>jdk</type>' \
    '<provides><version>17</version></provides>' \
    "<configuration><jdkHome>${JAVA_HOME}</jdkHome></configuration>" \
    '</toolchain></toolchains>' > /root/.m2/toolchains.xml

# Primero los pom solos: si no cambian las dependencias, Docker reutiliza esta capa.
COPY pom.xml .
COPY donaciones/pom.xml donaciones/
COPY logistica/pom.xml logistica/
RUN mvn -q -B -pl ${MODULO} -am dependency:go-offline

COPY ${MODULO}/src ${MODULO}/src
# El persistence.xml solo aporta la unidad de persistencia: la conexión real llega en runtime por
# DB_URL, DB_USER y DB_PASSWORD (ver ConfiguracionBase), así ninguna credencial queda en la imagen.
RUN cp ${MODULO}/src/main/resources/META-INF/persistence.example.xml \
       ${MODULO}/src/main/resources/META-INF/persistence.xml \
 && mvn -q -B -pl ${MODULO} -am package -DskipTests \
 && mvn -q -B -pl ${MODULO} dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib

# ---------- Ejecución ----------
FROM docker.io/library/eclipse-temurin:17-jre
ARG MODULO
ARG MAIN_CLASS
ENV MAIN_CLASS=${MAIN_CLASS}
WORKDIR /app
COPY --from=build /app/${MODULO}/target/${MODULO}-*.jar app.jar
COPY --from=build /app/${MODULO}/target/lib lib
ENTRYPOINT ["sh", "-c", "exec java -cp 'app.jar:lib/*' \"$MAIN_CLASS\""]
