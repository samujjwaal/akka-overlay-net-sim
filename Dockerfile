FROM openjdk:8

ENV SBT_VERSION 1.4.3

RUN \
  curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt

RUN sbt -Dsbt.rootdir=true update

WORKDIR /group11_project

COPY . /group11_project

CMD sbt run
