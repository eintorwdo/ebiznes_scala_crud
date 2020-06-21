# AlpineLinux with a glibc-2.23, Oracle Java 8, sbt and git
FROM rtfpessoa/ubuntu-jdk8

# Install sbt
RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list &&\
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add &&\
apt-get update &&\
apt-get install sbt


# Copy play project and compile it
# This will download all the ivy2 and sbt dependencies and install them
# in the container /root directory

ENV PROJECT_HOME /usr/src

COPY . /usr/src/backend
RUN cd usr/src/backend && \
    sbt compile

# Command
WORKDIR /usr/src/backend
CMD ["sbt", "run"]

# Expose code volume and play port 9000 

EXPOSE 9000

# EOF