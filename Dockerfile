FROM openjdk:jre-alpine
WORKDIR /opt/docker
COPY target/universal/stage /opt/docker/
RUN ["chown", "-R", "daemon:daemon", "."]
WORKDIR /opt/docker
USER daemon
ENTRYPOINT ["/opt/docker/bin/foundaml-server"]
CMD []
