ARG BUILD_IMAGE=usgs/java:11
ARG FROM_IMAGE=usgs/java:11

# === Stage 1: Compile and Build java codebase ===
FROM ${BUILD_IMAGE} as build

# install gradle
COPY ./gradlew /neic-traveltime/
COPY ./gradle /neic-traveltime/gradle
COPY ./build.gradle /neic-traveltime/.
WORKDIR /neic-traveltime
RUN ./gradlew tasks

# see .dockerignore for what is not COPYed
COPY . /neic-traveltime
# don't run tests and checks since this is a deployment
# container, we run these elsewhere in the pipeline
RUN ./gradlew --no-daemon build -x test -x check

# use consistent jar name
RUN cp /neic-traveltime/build/libs/neic-traveltime-*-all.jar /neic-traveltime/build/neic-traveltime-service.jar

# === Stage 2: Create image to serve java travel time service app ===
FROM ${FROM_IMAGE}

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