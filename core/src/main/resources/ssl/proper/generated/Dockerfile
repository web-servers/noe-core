# Small docker image specification to generate certificates on system that does include docker but not Java or OpenSSL
# docker build . -t "mydockerimage"
# docker run --rm -v $PWD/ca:/ca:z mydockerimage bash generate-trustchain.sh

FROM alpine

MAINTAINER Honza Kasik (https://github.com/honza-kasik)

RUN apk update && \
    apk add --no-cache openssl && \
    apk add --no-cache bash && \
    apk add --no-cache openjdk8-jre-base && \
    rm -rf /var/cache/apk/*

VOLUME /ca

COPY conf /conf
COPY generate-trustchain.sh /generate-trustchain.sh
