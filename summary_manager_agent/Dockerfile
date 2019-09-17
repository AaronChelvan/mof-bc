FROM openjdk:8
COPY summary_manager_agent/src/ /usr/src/myapp
COPY common/src/ /usr/src/myapp
WORKDIR /usr/src/myapp
RUN javac -cp ".:./leveldbjni-all-1.8.jar" SummaryManagerAgent.java Block.java Transaction.java
CMD ["java", "-cp", ".:./leveldbjni-all-1.8.jar", "SummaryManagerAgent"]

