---
tags: [ripple, property graph, graph databases, neo4j]
projects: [btcviz]
---

This repo translates Ripple transaction data from JSON format into a simple Neo4j graph database (for data visualization and analysis).

== You'll create

You'll use Neo4j's NoSQL graph-based data store to build an embedded Neo4j server, store Ripple transaction entities and relationships.

== You'll need

- Java 7
- Neo4J Community Edition 2.0.1
- Maven 3.0+
- one or more transaction files in json/ (sample transaction files are provided to start)

== Build an executable JAR

You can build a single executable JAR file that contains all the necessary dependencies, classes, and resources. This makes it easy to ship, version, and deploy the service as an application throughout the development lifecycle, across different environments, and so forth.


mvn clean package


Then you can run the JAR file:

java -jar target/rippletx-0.1.0.jar

 The procedure above will create a runnable JAR. You can also opt to build a classic WAR file instead.


== Run the service

You can run your service by typing: 

 mvn clean package && java -jar target/rippletx-0.1.0.jar.


Or alternatively:

mvn spring-boot:run


== Summary

Congratulations! You just used an embedded Neo4j server to store some simple related entities from the Ripple transaction history.  Now you can visualize the data in your web browser &/or run more advanced queries against the relations.

An example of this approach applied to BTC may be found at http://metabitco.in
