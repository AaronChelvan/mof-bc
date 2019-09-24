FROM openjdk:8
COPY node/src/ /usr/src/myapp
COPY common/src/ /usr/src/myapp
WORKDIR /usr/src/myapp
RUN javac -cp ".:./leveldbjni-all-1.8.jar:./jackson-core-2.9.0.jar:./jackson-annotations-2.9.0.jar:./jackson-databind-2.9.0.jar" Node.java Block.java Transaction.java
CMD ["java", "-cp", ".:./leveldbjni-all-1.8.jar:./jackson-core-2.9.0.jar:./jackson-annotations-2.9.0.jar:./jackson-databind-2.9.0.jar", "Node"]

