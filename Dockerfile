FROM openjdk:jre-alpine
WORKDIR /opt/docker
COPY app/target/universal/stage /opt/docker/
RUN apk add --update bash && rm -rf /var/cache/apk/*
RUN ["chown", "-R", "daemon:daemon", "."]
WORKDIR /opt/docker
USER daemon
EXPOSE 8080
ENTRYPOINT ["/opt/docker/bin/app"]
CMD []
