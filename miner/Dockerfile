FROM openjdk:8
COPY miner/src/ /usr/src/myapp
COPY common/src/ /usr/src/myapp
WORKDIR /usr/src/myapp
RUN javac -cp ".:./leveldbjni-all-1.8.jar" Miner.java Block.java Transaction.java
CMD ["java", "-cp", ".:./leveldbjni-all-1.8.jar", "Miner"]

