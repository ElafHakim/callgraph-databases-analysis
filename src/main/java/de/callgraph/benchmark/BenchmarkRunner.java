package de.callgraph.benchmark;

import de.callgraph.database.mysql.MySqlCallGraphRepository;
import de.callgraph.database.neo4j.Neo4jCallGraphRepository;
import de.callgraph.database.neo4j.Neo4jConnection;

import java.io.File;

public class BenchmarkRunner {

    private static final String CALL_GRAPH_DIRECTORY =
            "D:\\BA\\neuCG";

    public static void main(String[] args) {
        File directory = new File(CALL_GRAPH_DIRECTORY);

        validateDirectory(directory);

        try {
            runNeo4jBenchmark(directory);
            runMySqlBenchmark(directory);

            printResults();

        } finally {
            Neo4jConnection.closeConnection();
        }
    }

    private static void runNeo4jBenchmark(File directory) {
        System.out.println();
        System.out.println("------ Neo4j-Benchmark ------");

        Neo4jCallGraphRepository repository =
                new Neo4jCallGraphRepository();

        repository.importCallGraphs(directory);
    }

    private static void runMySqlBenchmark(File directory) {
        System.out.println();
        System.out.println("------ MySQL-Benchmark ------");

        MySqlCallGraphRepository repository =
                new MySqlCallGraphRepository();

        repository.importCallGraphs(directory);
    }

    private static void printResults() {
        long neo4jDuration = TimeUtil.getValue(0);
        long mySqlDuration = TimeUtil.getValue(1);

        System.out.println();
        System.out.println("------ Gesamtergebnis ------");
        System.out.println(
                "Neo4j: " + neo4jDuration + " Sekunden"
        );
        System.out.println(
                "MySQL: " + mySqlDuration + " Sekunden"
        );

        if (neo4jDuration < mySqlDuration) {
            System.out.println(
                    "Neo4j war beim Import schneller."
            );
        } else if (mySqlDuration < neo4jDuration) {
            System.out.println(
                    "MySQL war beim Import schneller."
            );
        } else {
            System.out.println(
                    "Beide Datenbanken waren gleich schnell."
            );
        }
    }

    private static void validateDirectory(File directory) {
        if (!directory.exists()) {
            throw new IllegalArgumentException(
                    "Der Callgraph-Ordner existiert nicht: "
                            + directory.getAbsolutePath()
            );
        }

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(
                    "Der angegebene Pfad ist kein Ordner: "
                            + directory.getAbsolutePath()
            );
        }
    }
}