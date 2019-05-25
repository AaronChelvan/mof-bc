FROM openjdk:7
COPY miner/src/ /usr/src/myapp
COPY common/src/ /usr/src/myapp
WORKDIR /usr/src/myapp
RUN javac Miner.java
CMD ["java", "Miner"]

