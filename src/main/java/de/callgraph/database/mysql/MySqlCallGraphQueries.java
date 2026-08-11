package de.callgraph.database.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import de.callgraph.benchmark.CsvFileOutput;

import static de.callgraph.database.mysql.MySqlConnection.getConnection;

public class MySqlCallGraphQueries {

    public static double mysqlQueryExecuteTime(String query) {
        try (Statement statement = getConnection().createStatement()) {

            statement.execute("SET PROFILING=1");
            statement.execute(query);

            try (ResultSet resultSet =
                         statement.executeQuery("SHOW PROFILES")) {

                double duration = 0.0;

                // Den letzten Profiling-Eintrag verwenden
                while (resultSet.next()) {
                    duration = resultSet.getDouble("Duration");
                }

                // Sekunden in Millisekunden umrechnen
                return duration * 1000;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    public static void main(String[] args) {

        CsvFileOutput csvFileOutput =
                new CsvFileOutput("mysql_queries");

        // Q1: Eine bestimmte Methode in einem Graphen finden
        double q1 = mysqlQueryExecuteTime(
                "SELECT nodes.name FROM nodes " +
                "WHERE name = \"getRepositorySession\" " +
                "AND declaredClass = " +
                "\"org/apache/maven/execution/MavenSession\" " +
                "AND graphName = \"maven-core-3.8.5-graph.json\";"
        );

        csvFileOutput.append("Q1", String.valueOf(q1));
        System.out.println("Q1: " + q1);

        // Q2: Methoden finden, die set über genau drei Kanten aufrufen
        double q2 = mysqlQueryExecuteTime(
                "SELECT n3.name " +
                "FROM edges e1 " +
                "INNER JOIN edges e2 " +
                "ON e2.targetNodeId = e1.nodeId " +
                "AND e2.graphName = e1.graphName " +
                "INNER JOIN edges e3 " +
                "ON e3.targetNodeId = e2.nodeId " +
                "AND e3.graphName = e2.graphName " +
                "INNER JOIN nodes n " +
                "ON e1.targetNodeId = n.nodeId " +
                "AND n.graphName = e1.graphName " +
                "INNER JOIN nodes n1 " +
                "ON e1.nodeId = n1.nodeId " +
                "AND n1.graphName = e1.graphName " +
                "INNER JOIN nodes n2 " +
                "ON e2.nodeId = n2.nodeId " +
                "AND n2.graphName = e2.graphName " +
                "INNER JOIN nodes n3 " +
                "ON e3.nodeId = n3.nodeId " +
                "AND n3.graphName = e3.graphName " +
                "WHERE n.name = \"set\" " +
                "AND n.declaredClass = \" java/util/BitSet\" " +
                "AND n.graphName = \"ant-1.10.12-graph.json\";"
        );

        csvFileOutput.append("Q2", String.valueOf(q2));
        System.out.println("Q2: " + q2);

        // Q3: Methoden finden, die set über maximal drei Kanten aufrufen
        double q3 = mysqlQueryExecuteTime(
                "SELECT n3.name, n2.name, n1.name " +
                "FROM edges e1 " +
                "LEFT JOIN edges e2 " +
                "ON e2.targetNodeId = e1.nodeId " +
                "AND e2.graphName = e1.graphName " +
                "LEFT JOIN edges e3 " +
                "ON e3.targetNodeId = e2.nodeId " +
                "AND e3.graphName = e2.graphName " +
                "LEFT JOIN nodes n " +
                "ON e1.targetNodeId = n.nodeId " +
                "AND e1.graphName = n.graphName " +
                "LEFT JOIN nodes n1 " +
                "ON e1.nodeId = n1.nodeId " +
                "AND e1.graphName = n1.graphName " +
                "LEFT JOIN nodes n2 " +
                "ON e2.nodeId = n2.nodeId " +
                "AND e2.graphName = n2.graphName " +
                "LEFT JOIN nodes n3 " +
                "ON e3.nodeId = n3.nodeId " +
                "AND e3.graphName = n3.graphName " +
                "WHERE n.name = \"set\" " +
                "AND n.declaredClass = \" java/util/BitSet\" " +
                "AND n.graphName = \"ant-1.10.12-graph.json\" " +
                "LIMIT 10;"
        );

        csvFileOutput.append("Q3", String.valueOf(q3));
        System.out.println("Q3: " + q3);

        // Q4: Methoden finden, die keine anderen Methoden aufrufen
        double q4 = mysqlQueryExecuteTime(
                "SELECT nodes.name " +
                "FROM nodes " +
                "WHERE nodes.graphName = " +
                "\"clojure-1.11.1-graph.json \" " +
                "AND nodes.nodeId NOT IN (" +
                "SELECT edges.nodeId FROM edges " +
                "WHERE edges.graphName = " +
                "\"clojure-1.11.1-graph.json \" " +
                "AND nodes.nodeId = edges.nodeId" +
                ");"
        );

        csvFileOutput.append("Q4", String.valueOf(q4));
        System.out.println("Q4: " + q4);

        // Q5: Methoden finden, die von keiner Methode aufgerufen werden
        double q5 = mysqlQueryExecuteTime(
                "SELECT nodes.name " +
                "FROM nodes " +
                "WHERE nodes.graphName = " +
                "\"jackson-core-2.13.2-graph.json \" " +
                "AND nodes.nodeId NOT IN (" +
                "SELECT edges.targetNodeId FROM edges " +
                "WHERE nodes.nodeId = edges.targetNodeId" +
                ");"
        );

        csvFileOutput.append("Q5", String.valueOf(q5));
        System.out.println("Q5: " + q5);

        // Q6: Methoden finden, die zwei bestimmte Methoden aufrufen
        double q6 = mysqlQueryExecuteTime(
                "SELECT n.name " +
                "FROM edges e1 " +
                "INNER JOIN nodes n1 " +
                "ON e1.targetNodeId = n1.nodeId " +
                "INNER JOIN edges e2 " +
                "ON e2.nodeId = e1.nodeId " +
                "INNER JOIN nodes n2 " +
                "ON n2.nodeId = e2.targetNodeId " +
                "INNER JOIN nodes n " +
                "ON n.nodeId = e1.nodeId " +
                "WHERE n1.name = \"put\" " +
                "AND n1.declaredClass = \"java/util/Map\" " +
                "AND n1.graphName = \"args4j-2.33-graph.json\" " +
                "AND n2.name = \"setUsageWidth\" " +
                "AND n2.declaredClass = " +
                "\"org/kohsuke/args4j/CmdLineParse\" " +
                "AND n2.graphName = \"args4j-2.33-graph.json\";"
        );

        csvFileOutput.append("Q6", String.valueOf(q6));
        System.out.println("Q6: " + q6);

        // Q7: Anzahl der mit CHA erzeugten Relationen
        double q7 = mysqlQueryExecuteTime(
                "SELECT count(edgeId) " +
                "FROM edges " +
                "WHERE edges.algorithm = \"cha\";"
        );

        csvFileOutput.append("Q7", String.valueOf(q7));
        System.out.println("Q7: " + q7);

        // Q8: Anzahl der Methoden, die eine bestimmte Methode aufrufen
        double q8 = mysqlQueryExecuteTime(
                "SELECT count(n1.name) " +
                "FROM edges e1 " +
                "INNER JOIN nodes n " +
                "ON e1.targetNodeId = n.nodeId " +
                "AND n.graphName = e1.graphName " +
                "INNER JOIN nodes n1 " +
                "ON e1.nodeId = n1.nodeId " +
                "AND n1.graphName = e1.graphName " +
                "WHERE n.name = \"_findCustomBeanDeserializer\" " +
                "AND n.declaredClass = " +
                "\"com/fasterxml/jackson/databind/deser/" +
                "BasicDeserializerFactory\" " +
                "AND n.graphName = " +
                "\"jackson-databind-2.13.2.2-graph\";"
        );

        csvFileOutput.append("Q8", String.valueOf(q8));
        System.out.println("Q8: " + q8);
    }
}