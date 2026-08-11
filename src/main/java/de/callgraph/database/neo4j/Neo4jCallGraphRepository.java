package de.callgraph.database.neo4j;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.neo4j.driver.Values.parameters;

import de.callgraph.benchmark.CsvFileOutput;
import de.callgraph.benchmark.TimeUtil;

public class Neo4jCallGraphRepository {

    private final Driver driver;

    public Neo4jCallGraphRepository() {
        this.driver = Neo4jConnection.getDriver();
    }

    public void importCallGraphs(File directory) {
        File[] files = directory.listFiles(
                file -> file.isFile()
                        && file.getName().endsWith(".json")
        );

        if (files == null) {
            throw new IllegalArgumentException(
                    "Der Ordner konnte nicht gelesen werden: "
                            + directory.getAbsolutePath()
            );
        }

        CsvFileOutput csvFileOutput =
                new CsvFileOutput("neo4j_lauftest2_clojure");

        JSONParser parser = new JSONParser();
        long durationOfAllGraphs = 0;

        System.out.println("----- Start -----");

        for (File callGraphFile : files) {
            long durationSeconds = importCallGraph(
                    callGraphFile,
                    parser
            );

            csvFileOutput.append(
                    callGraphFile.getName(),
                    String.valueOf(durationSeconds)
            );

            System.out.println(
                    callGraphFile.getName()
                            + " "
                            + durationSeconds
                            + " Sekunden"
            );

            durationOfAllGraphs += durationSeconds;
        }

        /*
         * Nur behalten, wenn TimeUtil bereits in das neue Projekt
         * übernommen wurde.
         */
        // TimeUtil.setValue(0, durationOfAllGraphs);

        System.out.println(
                "Gesamtdauer: "
                        + durationOfAllGraphs
                        + " Sekunden"
        );
    }

    private long importCallGraph(
            File callGraphFile,
            JSONParser parser
    ) {
        try (FileReader reader = new FileReader(callGraphFile)) {
            JSONArray methods = (JSONArray) parser.parse(reader);
            String graphName = callGraphFile.getName();

            long startTime = System.currentTimeMillis();

            createNodes(methods, graphName);
            createEdges(methods, graphName);

            return (System.currentTimeMillis() - startTime) / 1000;

        } catch (IOException | ParseException e) {
            throw new IllegalStateException(
                    "Der Callgraph konnte nicht gelesen werden: "
                            + callGraphFile.getAbsolutePath(),
                    e
            );
        }
    }

    private void createNodes(
            JSONArray methods,
            String graphName
    ) {
        try (Session session = driver.session()) {
            for (Object methodObject : methods) {
                JSONObject method = (JSONObject) methodObject;

                long id = (long) method.get("id");
                String name = (String) method.get("name");
                String descriptor =
                        (String) method.get("descriptor");
                long paramCnt = (long) method.get("paramCnt");
                String declaredClass =
                        (String) method.get("declaredClass");

                session.run(
                        "CREATE (n:Method {" +
                                "uid: $uid, " +
                                "graphName: $graphName, " +
                                "name: $name, " +
                                "id: $id, " +
                                "descriptor: $descriptor, " +
                                "paramCnt: $paramCnt, " +
                                "declaredClass: $declaredClass" +
                                "})",
                        parameters(
                                "uid", graphName + id,
                                "graphName", graphName,
                                "name", name,
                                "id", id,
                                "descriptor", descriptor,
                                "paramCnt", paramCnt,
                                "declaredClass", declaredClass
                        )
                ).consume();
            }

            createNodeIndexes(session);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Die Neo4j-Knoten konnten nicht erstellt werden.",
                    e
            );
        }
    }

    private void createNodeIndexes(Session session) {
        session.run(
                "CREATE INDEX nodeIndex IF NOT EXISTS " +
                        "FOR (n:Method) " +
                        "ON (n.graphName, n.name, n.declaredClass)"
        ).consume();

        session.run(
                "CREATE INDEX graphNameIndex IF NOT EXISTS " +
                        "FOR (n:Method) ON (n.graphName)"
        ).consume();

        session.run(
                "CREATE CONSTRAINT uniqueConstraint IF NOT EXISTS " +
                        "FOR (n:Method) REQUIRE n.uid IS UNIQUE"
        ).consume();
    }

    private void createEdges(
            JSONArray methods,
            String graphName
    ) {
        try (Session session = driver.session()) {
            for (Object methodObject : methods) {
                JSONObject method = (JSONObject) methodObject;

                long sourceId = (long) method.get("id");
                JSONArray invocations =
                        (JSONArray) method.get("invocations");

                for (Object invocationObject : invocations) {
                    JSONObject invocation =
                            (JSONObject) invocationObject;

                    String algorithm =
                            (String) invocation.get("algorithm");

                    long targetId =
                            (long) invocation.get("targetNode");

                    session.run(
                            "MATCH " +
                                    "(source:Method {uid: $sourceUid}), " +
                                    "(target:Method {uid: $targetUid}) " +
                                    "CREATE (source)-[" +
                                    ":calls {algorithm: $algorithm}" +
                                    "]->(target)",
                            parameters(
                                    "sourceUid", graphName + sourceId,
                                    "targetUid", graphName + targetId,
                                    "algorithm", algorithm
                            )
                    ).consume();
                }
            }

            session.run(
                    "CREATE INDEX algorithmIndex IF NOT EXISTS " +
                            "FOR ()-[r:calls]-() ON (r.algorithm)"
            ).consume();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Die Neo4j-Kanten konnten nicht erstellt werden.",
                    e
            );
        }
    }

    public static void main(String[] args) {
        Neo4jCallGraphRepository repository =
                new Neo4jCallGraphRepository();

        try {
            repository.importCallGraphs(
                    new File("D:\\BA\\neuCG")
            );
        } finally {
            Neo4jConnection.closeConnection();
        }
    }
}