FROM openjdk:7
COPY node/src/ /usr/src/myapp
COPY common/src/ /usr/src/myapp
WORKDIR /usr/src/myapp
RUN javac Node.java
CMD ["java", "Node"]

