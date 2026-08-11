package de.callgraph.database.neo4j;

import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;

import java.util.concurrent.TimeUnit;

public class Neo4jCallGraphQueries {

    public static double neo4jQueryExecuteTime(String query) {
        try (Session session =
                     Neo4jConnection.getDriver().session()) {

            ResultSummary summary =
                    session.run(query).consume();

            return (double) summary.resultAvailableAfter(
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public static void main(String[] args) {

        CsvFileOutput csvFileOutput =
                new CsvFileOutput("neo4j_queries");

        // Q1: Methode getRepositorySession in MavenSession finden
        double q1 = neo4jQueryExecuteTime(
                "MATCH (m:Method {" +
                        "name: \"getRepositorySession\", " +
                        "declaredClass: \" org/apache/maven/execution/MavenSession\", " +
                        "graphName: \"maven-core-3.8.5-graph.json\"" +
                        "}) RETURN m"
        );

        csvFileOutput.append("Q1", String.valueOf(q1));
        System.out.println("Q1: " + q1);

        // Q2: Methoden finden, die set indirekt über genau drei Kanten aufrufen
        double q2 = neo4jQueryExecuteTime(
                "MATCH (m:Method)-[:calls*3]->" +
                        "(x:Method {" +
                        "name: \"set\", " +
                        "declaredClass: \" java/util/BitSet\", " +
                        "graphName: \"ant-1.10.12-graph.json\"" +
                        "}) RETURN m LIMIT 10"
        );

        csvFileOutput.append("Q2", String.valueOf(q2));
        System.out.println("Q2: " + q2);

        // Q3: Methoden finden, die set über maximal drei Kanten aufrufen
        double q3 = neo4jQueryExecuteTime(
                "MATCH (x:Method {" +
                        "name: \"set\", " +
                        "declaredClass: \" java/util/BitSet\", " +
                        "graphName: \"ant-1.10.12-graph.json\"" +
                        "})<-[:calls*1..3]-(m:Method) " +
                        "RETURN m LIMIT 10"
        );

        csvFileOutput.append("Q3", String.valueOf(q3));
        System.out.println("Q3: " + q3);

        // Q4: Methoden finden, die keine anderen Methoden aufrufen
        double q4 = neo4jQueryExecuteTime(
                "MATCH (a:Method {" +
                        "graphName: \"clojure-1.11.1-graph.json\"" +
                        "}) " +
                        "WHERE NOT (a)-[:calls]->() " +
                        "RETURN a"
        );

        csvFileOutput.append("Q4", String.valueOf(q4));
        System.out.println("Q4: " + q4);

        // Q5: Methoden finden, die von keiner anderen Methode aufgerufen werden
        double q5 = neo4jQueryExecuteTime(
                "MATCH (a:Method {" +
                        "graphName: \"jackson-core-2.13.2-graph.json\"" +
                        "}) " +
                        "WHERE NOT (a)<-[:calls]-(:Method) " +
                        "RETURN a"
        );

        csvFileOutput.append("Q5", String.valueOf(q5));
        System.out.println("Q5: " + q5);

        // Q6: Methoden finden, die zwei bestimmte Methoden aufrufen
        double q6 = neo4jQueryExecuteTime(
                "MATCH (m:Method) " +
                        "WHERE (m)-[:calls]->(:Method {" +
                        "name: \"put\", " +
                        "declaredClass: \" java/util/Map\", " +
                        "graphName: \"args4j-2.33-graph.json\"" +
                        "}) " +
                        "AND (m)-[:calls]->(:Method {" +
                        "name: \"setUsageWidth\", " +
                        "declaredClass: \"org/kohsuke/args4j/CmdLineParser\", " +
                        "graphName: \"args4j-2.33-graph.json\"" +
                        "}) " +
                        "RETURN m"
        );

        csvFileOutput.append("Q6", String.valueOf(q6));
        System.out.println("Q6: " + q6);

        // Q7: Anzahl der mit CHA erzeugten Relationen
        double q7 = neo4jQueryExecuteTime(
                "MATCH ()-[r:calls {algorithm: \"cha\"}]->() " +
                        "RETURN count(r)"
        );

        csvFileOutput.append("Q7", String.valueOf(q7));
        System.out.println("Q7: " + q7);

        // Q8: Anzahl der Methoden, die findEnum aufrufen
        double q8 = neo4jQueryExecuteTime(
                "MATCH (m:Method)-[:calls]->" +
                        "(x:Method {" +
                        "name: \"findEnum\", " +
                        "declaredClass: " +
                        "\"com/fasterxml/jackson/databind/util/EnumResolver\", " +
                        "graphName: " +
                        "\"jackson-databind-2.13.2.2-graph\"" +
                        "}) RETURN count(m)"
        );

        csvFileOutput.append("Q8", String.valueOf(q8));
        System.out.println("Q8: " + q8);

        neo4jDriver.close();
    }
}