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

FROM ubuntu:latest

COPY --from=jdk /jlinked jdk
COPY entities.jar .

ENTRYPOINT ["/jdk/bin/java", "-jar", "entities.jar"]
