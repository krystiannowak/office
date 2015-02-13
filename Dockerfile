FROM java

ADD target/office-jar-with-dependencies.jar /project/office-jar-with-dependencies.jar

WORKDIR /project

CMD java -jar office-jar-with-dependencies.jar
