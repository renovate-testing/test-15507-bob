# Copyright 2018-2022 Rahul De
#
# Use of this source code is governed by an MIT-style
# license that can be found in the LICENSE file or at
# https://opensource.org/licenses/MIT.

FROM eclipse-temurin:18-jdk AS jdk

RUN jlink \
    --compress=2 \
    --module-path jmods \
    --add-modules java.base,java.sql,jdk.unsupported,java.naming,java.desktop,java.management,java.net.http \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --output jlinked

# Based on Fedora
FROM quay.io/podman/stable:latest

COPY --from=jdk /jlinked jdk
COPY runner.jar .
RUN echo -e "set -e \n\
podman system service -t 0 unix:///tmp/pod.sock & \n\
/jdk/bin/java -jar runner.jar" > entrypoint.sh
ENV CONTAINER_ENGINE_URL "unix:///tmp/pod.sock"

ENTRYPOINT ["sh", "entrypoint.sh"]
