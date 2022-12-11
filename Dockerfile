FROM amazoncorretto:17
COPY . /app
WORKDIR /app
RUN ./gradlew stage

CMD ["./build/install/poly3/bin/poly3"]

#FROM ubuntu:latest
#COPY --from=0 build/install/poly3/bin/poly3 app
#CMD ["./app"]