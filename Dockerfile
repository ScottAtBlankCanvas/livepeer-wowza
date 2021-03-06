FROM wowzamedia/wowza-streaming-engine-linux@sha256:6613579af41950cd26593f582af285ab221b5a7119248b4314317c2c389e96a4 as builder

RUN apt-get update && apt-get install -y curl
# Wowza v4.7.8 runs on Java 9? Well, okay...
RUN curl -LO https://github.com/AdoptOpenJDK/openjdk9-binaries/releases/download/jdk-9.0.4%2B11/OpenJDK9U-jdk_x64_linux_hotspot_9.0.4_11.tar.gz \
  && tar -xf OpenJDK9U-jdk_x64_linux_hotspot_9.0.4_11.tar.gz \
  && mkdir /usr/lib/jvm \
  && mv jdk-9.0.4+11 /usr/lib/jvm/jdk-9.0.4+11

ENV JAVA_HOME /usr/lib/jvm/jdk-9.0.4+11
ENV PATH $PATH:/usr/lib/jvm/jdk-9.0.4+11/bin

RUN apt-get update && apt-get install -y curl
# Ubuntu ships Ant with a Java 11 dependency, so we grab it manually.
WORKDIR /ant
ENV PATH $PATH:/ant/apache-ant-1.10.6/bin
RUN curl -o ant.tar.gz https://archive.apache.org/dist/ant/binaries/apache-ant-1.10.6-bin.tar.gz && tar xzf ant.tar.gz
WORKDIR /build/wowza
RUN mkdir /build/bin
ADD build.xml build.xml
ENV PATH $PATH:/usr/local/WowzaStreamingEngine/jre/bin
ADD build-trust-store.sh build-trust-store.sh
RUN ./build-trust-store.sh /usr/local/WowzaStreamingEngine/jre
RUN cp /usr/local/WowzaStreamingEngine/jre/lib/security/cacerts ./livepeer_cacerts
ARG version
ENV VERSION=$version
ADD vendor vendor
ADD src src
RUN sed -i "s/UNKNOWN_VERSION/${VERSION}/g" src/org/livepeer/LivepeerWowza/LivepeerVersion.java
RUN mkdir bin && ant -lib /usr/local/WowzaStreamingEngine/lib -lib ./vendor -Dwowza.lib.dir=/usr/local/WowzaStreamingEngine/lib

FROM golang:1.12-buster as installer
RUN apt-get update && apt-get install -y libxml2-dev libonig-dev liblzma-dev zlib1g-dev libgmp-dev libicu-dev
WORKDIR /go/src/github.com/livepeer/livepeer-wowza
ADD installation_script.go installation_script.go
RUN go get .
# -static -lm -lz -ldl -licuuc -licudata
RUN go build -tags netgo -o install_livepeer_wowza -ldflags '-extldflags "-static -lz -licuuc -llzma  -licudata -lm -ldl -lstdc++ -lxml2"' installation_script.go
RUN tar czvf install_livepeer_wowza.linux.tar.gz install_livepeer_wowza

FROM wowzamedia/wowza-streaming-engine-linux@sha256:6613579af41950cd26593f582af285ab221b5a7119248b4314317c2c389e96a4 as install-test
RUN apt-get update && apt-get install -y ca-certificates
COPY --from=installer /go/src/github.com/livepeer/livepeer-wowza/install_livepeer_wowza /install_livepeer_wowza
RUN /install_livepeer_wowza -apikey abc123

# For testing on Java 8/WSE 4.7.7: wowzamedia/wowza-streaming-engine-linux@sha256:904d95965cfdbec477a81374fcd22dfc48db1972e690dacb80d2114a8d597f95
FROM wowzamedia/wowza-streaming-engine-linux@sha256:6613579af41950cd26593f582af285ab221b5a7119248b4314317c2c389e96a4 as server

RUN apt-get update \
  && apt-get install -y software-properties-common \
  && add-apt-repository ppa:savoury1/backports \
  && add-apt-repository ppa:savoury1/graphics \
  && add-apt-repository ppa:savoury1/multimedia \
  && add-apt-repository ppa:savoury1/backports \
  && add-apt-repository ppa:savoury1/ffmpeg4 \
  && apt-get update \
  && apt-get install -y ffmpeg

COPY --from=installer /go/src/github.com/livepeer/livepeer-wowza/install_livepeer_wowza.linux.tar.gz /usr/local/install_livepeer_wowza.linux.tar.gz

ADD etc/WowzaStreamingEngine.conf /etc/supervisor/conf.d/WowzaStreamingEngine.conf
ADD etc/WowzaStreamingEngineManager.conf /etc/supervisor/conf.d/WowzaStreamingEngineManager.conf
ADD etc/Server.xml /usr/local/WowzaStreamingEngine/conf/Server.xml
ADD etc/Application.xml /usr/local/WowzaStreamingEngine/conf/live/Application.xml
ADD etc/lp_transcode.xml /usr/local/WowzaStreamingEngine/transcoder/templates/lp_transcode.xml
ADD etc/camcast_transcode.xml /usr/local/WowzaStreamingEngine/transcoder/templates/camcast.xml
ADD etc/camcast.stream /usr/local/WowzaStreamingEngine/content/camcast.stream
ADD etc/print-threads.sh /usr/local/bin/print-threads.sh
COPY --from=builder /usr/local/WowzaStreamingEngine/lib/LivepeerWowza.jar /usr/local/WowzaStreamingEngine/lib/LivepeerWowza.jar
RUN chown wowza:wowza /usr/local/WowzaStreamingEngine/lib/LivepeerWowza.jar && chmod 775 /usr/local/WowzaStreamingEngine/lib/LivepeerWowza.jar

FROM wowzamedia/wowza-streaming-engine-linux@sha256:6613579af41950cd26593f582af285ab221b5a7119248b4314317c2c389e96a4 as tester

WORKDIR /wowza-testing
RUN apt-get update && apt-get install -y ca-certificates curl ffmpeg bc
ADD etc/WowzaStreamingEngine.conf /etc/supervisor/conf.d/WowzaStreamingEngine.conf
ADD etc/WowzaStreamingEngineManager.conf /etc/supervisor/conf.d/WowzaStreamingEngineManager.conf
ADD etc/Server.xml /usr/local/WowzaStreamingEngine/conf/Server.xml
ADD etc/Application.xml /usr/local/WowzaStreamingEngine/conf/live/Application.xml
RUN curl -o ./official_test_source_2s_keys_24pfs.mp4 https://storage.googleapis.com/lp_testharness_assets/official_test_source_2s_keys_24pfs.mp4
COPY --from=builder /usr/local/WowzaStreamingEngine/lib/LivepeerWowza.jar /usr/local/WowzaStreamingEngine/lib/LivepeerWowza.jar
ADD ./wowza-license ./wowza-license
ADD ./wowza-e2e-test.sh ./wowza-e2e-test.sh
CMD WSE_LIC="$(cat ./wowza-license)" ./wowza-e2e-test.sh
