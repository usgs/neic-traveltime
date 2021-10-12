ARG BUILD_IMAGE=usgs/centos:7
ARG FROM_IMAGE=usgs/centos:7

# === Stage 1: Compile and Build java codebase ===
FROM ${BUILD_IMAGE} as build

# install java; which is used by gradle to find java
RUN yum install -y java-11-openjdk-devel which

# install gradle
COPY ./gradlew /neic-traveltime/
COPY ./gradle /neic-traveltime/gradle
COPY ./build.gradle /neic-traveltime/.
WORKDIR /neic-traveltime
RUN ./gradlew tasks

# see .dockerignore for what is not COPYed
COPY . /neic-traveltime
RUN ./gradlew --no-daemon build -x test

# use consistent jar name
RUN cp /neic-traveltime/build/libs/neic-traveltime-*-all.jar /neic-traveltime/build/neic-traveltime-service.jar

# === Stage 2: Create image to serve java travel time service app ===
FROM ${FROM_IMAGE}

# install java
RUN yum install -y java-11-openjdk-headless

# copy shadow jar
COPY --from=build /neic-traveltime/build/neic-traveltime-service.jar /neic-traveltime/
# copy models
COPY --from=build /neic-traveltime/build/models /neic-traveltime/models
# copy entrypoint
COPY --from=build /neic-traveltime/docker-entrypoint.sh /neic-traveltime/

# set environment
ENV traveltime.model.path=/neic-traveltime/models/
ENV traveltime.serialized.path=/neic-traveltime/local/

# run as root to avoid volume writing issues
USER root
WORKDIR /neic-traveltime

# create entrypoint, needs double quotes
ENTRYPOINT [ "/neic-traveltime/docker-entrypoint.sh" ]
EXPOSE 8080