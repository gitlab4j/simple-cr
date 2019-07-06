# Use the latest Alpine OpenJDK 8 image
LABEL maintainer "simple-cr@gitlab4j.org"

FROM openjdk:8-alpine
ARG BUILD_VERSION


ENV SIMPLE_CR_DIR /usr/local/simple-cr
RUN mkdir -p $SIMPLE_CR_DIR/dbdata $SIMPLE_CR_DIR/lib
ADD simple-cr-${BUILD_VERSION}.jar $SIMPLE_CR_DIR/lib/simple-cr.jar

VOLUME $SIMPLE_CR_DIR/dbdata
EXPOSE 8080
WORKDIR $SIMPLE_CR_DIR

CMD java $JAVA_OPTS -jar $SIMPLE_CR_DIR/lib/simple-cr.jar
