package de.callgraph.database.neo4j;

import de.callgraph.benchmark.CsvFileOutput;
import de.callgraph.benchmark.TimeUtil;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.neo4j.driver.Values.parameters;

public final class Neo4j implements AutoCloseable {

    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private static final String URI =
            getRequiredValue("NEO4J_URI");

    private static final String USER =
            getRequiredValue("NEO4J_USER");

    private static final String PASSWORD =
            getRequiredValue("NEO4J_PASSWORD");

    private static final String CALL_GRAPH_DIRECTORY =
            getRequiredValue("CALL_GRAPH_DIRECTORY");

    private static Driver neo4jDriver;

    private static String graphName;
    private static JSONArray jsonArr;

    private Neo4j() {
        // Verhindert das Erzeugen von Objekten dieser Klasse
    }

    public static void createDriver() {
        if (neo4jDriver != null) {
            return;
        }

        try {
            neo4jDriver = GraphDatabase.driver(
                    URI,
                    AuthTokens.basic(USER, PASSWORD)
            );

            neo4jDriver.verifyConnectivity();

        } catch (Exception e) {
            closeConnection();

            throw new IllegalStateException(
                    "Die Neo4j-Verbindung konnte nicht hergestellt werden.",
                    e
            );
        }
    }

    public static void main(String... args) {
        createDriver();

        CsvFileOutput csvFileOutput =
                new CsvFileOutput("neo4j-lauftest2-clojure");

        JSONParser parser = new JSONParser();

        try {
            File directory = new File(CALL_GRAPH_DIRECTORY);
            File[] directoryListing = directory.listFiles();

            if (directoryListing == null) {
                throw new IllegalArgumentException(
                        "Der Callgraph-Ordner konnte nicht gelesen werden: "
                                + directory.getAbsolutePath()
                );
            }

            long durationOfAllGraphs = 0;

            System.out.println("----- Start -----");

            for (File callGraphFile : directoryListing) {
                if (!callGraphFile.isFile()) {
                    continue;
                }

                graphName = callGraphFile.getName();

                try (FileReader reader = new FileReader(callGraphFile)) {
                    jsonArr = (JSONArray) parser.parse(reader);
                }

                long startTime = System.currentTimeMillis();

                createNodes();
                createEdges();

                long durationSeconds =
                        (System.currentTimeMillis() - startTime) / 1000;

                csvFileOutput.append(
                        callGraphFile.getName(),
                        String.valueOf(durationSeconds)
                );

                System.out.println(
                        graphName + " " + durationSeconds + " Sekunden"
                );

                durationOfAllGraphs += durationSeconds;
            }

            TimeUtil.setValue(0, durationOfAllGraphs);

        } catch (IOException | ParseException e) {
            throw new IllegalStateException(
                    "Die Callgraph-Dateien konnten nicht verarbeitet werden.",
                    e
            );

        } finally {
            closeConnection();
        }
    }

    private static void createNodes() {
        try (Session session = neo4jDriver.session()) {
            for (Object element : jsonArr) {
                JSONObject method = (JSONObject) element;

                long id = (long) method.get("id");
                String name = (String) method.get("name");
                String descriptor = (String) method.get("descriptor");
                long paramCnt = (long) method.get("paramCnt");
                String declaredClass =
                        (String) method.get("declaredClass");

                session.run(
                        """
                        CREATE (n:Method {
                            uid: $uid,
                            graphName: $graphName,
                            name: $name,
                            id: $id,
                            descriptor: $descriptor,
                            paramCnt: $paramCnt,
                            declaredClass: $declaredClass
                        })
                        """,
                        parameters(
                                "uid", graphName + id,
                                "graphName", graphName,
                                "name", name,
                                "id", id,
                                "descriptor", descriptor,
                                "paramCnt", paramCnt,
                                "declaredClass", declaredClass
                        )
                );
            }

            session.run(
                    """
                    CREATE INDEX nodeIndex IF NOT EXISTS
                    FOR (n:Method)
                    ON (n.graphName, n.name, n.declaredClass)
                    """
            );

            session.run(
                    """
                    CREATE INDEX graphNameIndex IF NOT EXISTS
                    FOR (n:Method)
                    ON (n.graphName)
                    """
            );

            session.run(
                    """
                    CREATE CONSTRAINT uniqueConstraint IF NOT EXISTS
                    FOR (n:Method)
                    REQUIRE n.uid IS UNIQUE
                    """
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Die Neo4j-Knoten konnten nicht erstellt werden.",
                    e
            );
        }
    }

    private static void createEdges() {
        try (Session session = neo4jDriver.session()) {
            for (Object element : jsonArr) {
                JSONObject method = (JSONObject) element;

                long sourceId = (long) method.get("id");
                JSONArray invocations =
                        (JSONArray) method.get("invocations");

                if (invocations == null || invocations.isEmpty()) {
                    continue;
                }

                for (Object invocationElement : invocations) {
                    JSONObject invocation =
                            (JSONObject) invocationElement;

                    String algorithm =
                            (String) invocation.get("algorithm");

                    long targetNode =
                            (long) invocation.get("targetNode");

                    session.run(
                            """
                            MATCH
                                (source:Method {uid: $sourceUid}),
                                (target:Method {uid: $targetUid})
                            CREATE
                                (source)-[:calls {
                                    algorithm: $algorithm
                                }]->(target)
                            """,
                            parameters(
                                    "sourceUid", graphName + sourceId,
                                    "targetUid", graphName + targetNode,
                                    "algorithm", algorithm
                            )
                    );
                }
            }

            session.run(
                    """
                    CREATE INDEX algorithmIndex IF NOT EXISTS
                    FOR ()-[relationship:calls]-()
                    ON (relationship.algorithm)
                    """
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Die Neo4j-Kanten konnten nicht erstellt werden.",
                    e
            );
        }
    }

    public static void closeConnection() {
        if (neo4jDriver != null) {
            neo4jDriver.close();
            neo4jDriver = null;
        }
    }

    @Override
    public void close() {
        closeConnection();
    }

    private static String getRequiredValue(String variableName) {
        String value = DOTENV.get(variableName);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Die Umgebungsvariable "
                            + variableName
                            + " wurde nicht festgelegt."
            );
        }

        return value;
    }
}