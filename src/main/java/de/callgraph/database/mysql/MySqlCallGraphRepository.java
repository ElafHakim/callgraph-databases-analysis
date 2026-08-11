package de.callgraph.database.mysql;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import de.callgraph.benchmark.CsvFileOutput;
import de.callgraph.benchmark.TimeUtil;

public class MySqlCallGraphRepository {

    private static final int BATCH_SIZE = 1000;

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
                new CsvFileOutput("mysql_lauftest_2_clojure");

        JSONParser parser = new JSONParser();
        long totalDurationSeconds = 0;

        System.out.println("------ Start ------");

        try (Connection connection =
                     MySqlConnection.getConnection()) {

            createTables(connection);

            for (File callGraphFile : files) {
                long durationSeconds = importCallGraph(
                        connection,
                        callGraphFile,
                        parser
                );

                totalDurationSeconds += durationSeconds;

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
            }

            createForeignKeys(connection);

            // Nur verwenden, wenn TimeUtil im Projekt vorhanden ist
            // TimeUtil.setValue(1, totalDurationSeconds);

            System.out.println(
                    "Gesamtdauer: "
                            + totalDurationSeconds
                            + " Sekunden"
            );

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Fehler beim Import in MySQL.",
                    e
            );
        }
    }

    private long importCallGraph(
            Connection connection,
            File callGraphFile,
            JSONParser parser
    ) {
        try (FileReader reader = new FileReader(callGraphFile)) {
            JSONArray methods = (JSONArray) parser.parse(reader);
            String graphName = callGraphFile.getName();

            long startTime = System.currentTimeMillis();

            insertNodes(connection, methods, graphName);
            insertEdges(connection, methods, graphName);

            return (System.currentTimeMillis() - startTime) / 1000;

        } catch (IOException | ParseException e) {
            throw new IllegalStateException(
                    "Die Callgraph-Datei konnte nicht gelesen werden: "
                            + callGraphFile.getAbsolutePath(),
                    e
            );
        }
    }

    private void createTables(Connection connection)
            throws SQLException {

        String nodesTable =
                "CREATE TABLE IF NOT EXISTS nodes (" +
                        "graphName VARCHAR(255) NOT NULL, " +
                        "nodeId BIGINT NOT NULL, " +
                        "name TEXT, " +
                        "descriptor VARCHAR(999), " +
                        "paramCnt BIGINT, " +
                        "declaredClass VARCHAR(999), " +
                        "CONSTRAINT pk_graphName_nodeId " +
                        "PRIMARY KEY (graphName, nodeId), " +
                        "INDEX idx1 (graphName), " +
                        "INDEX idx2 (nodeId), " +
                        "INDEX idx3 (name(20)), " +
                        "INDEX idx4 (declaredClass(50))" +
                        ")";

        String edgesTable =
                "CREATE TABLE IF NOT EXISTS edges (" +
                        "edgeId BIGINT NOT NULL AUTO_INCREMENT, " +
                        "graphName VARCHAR(255) NOT NULL, " +
                        "nodeId BIGINT NOT NULL, " +
                        "targetNodeId BIGINT NOT NULL, " +
                        "algorithm VARCHAR(255), " +
                        "PRIMARY KEY (edgeId), " +
                        "INDEX idx5 (graphName), " +
                        "INDEX idx6 (nodeId), " +
                        "INDEX idx7 (targetNodeId), " +
                        "INDEX idx8 (algorithm)" +
                        ")";

        try (Statement statement = connection.createStatement()) {
            statement.execute(nodesTable);
            statement.execute(edgesTable);
        }
    }

    private void insertNodes(
            Connection connection,
            JSONArray methods,
            String graphName
    ) throws SQLException {

        String insertNode =
                "INSERT INTO nodes " +
                        "(graphName, nodeId, name, descriptor, " +
                        "paramCnt, declaredClass) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(insertNode)) {

            int batchCounter = 0;

            for (Object methodObject : methods) {
                JSONObject method = (JSONObject) methodObject;

                long nodeId = (long) method.get("id");
                String name = (String) method.get("name");
                String descriptor =
                        (String) method.get("descriptor");
                long paramCnt = (long) method.get("paramCnt");
                String declaredClass =
                        (String) method.get("declaredClass");

                statement.setString(1, graphName);
                statement.setLong(2, nodeId);
                statement.setString(3, name);
                statement.setString(4, descriptor);
                statement.setLong(5, paramCnt);
                statement.setString(6, declaredClass);
                statement.addBatch();

                batchCounter++;

                if (batchCounter == BATCH_SIZE) {
                    statement.executeBatch();
                    batchCounter = 0;
                }
            }

            if (batchCounter > 0) {
                statement.executeBatch();
            }
        }
    }

    private void insertEdges(
            Connection connection,
            JSONArray methods,
            String graphName
    ) throws SQLException {

        /*
         * edgeId wird nicht angegeben, weil MySQL sie durch
         * AUTO_INCREMENT automatisch erzeugt.
         */
        String insertEdge =
                "INSERT INTO edges " +
                        "(graphName, nodeId, targetNodeId, algorithm) " +
                        "VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(insertEdge)) {

            int batchCounter = 0;

            for (Object methodObject : methods) {
                JSONObject method = (JSONObject) methodObject;

                long sourceNodeId = (long) method.get("id");
                JSONArray invocations =
                        (JSONArray) method.get("invocations");

                for (Object invocationObject : invocations) {
                    JSONObject invocation =
                            (JSONObject) invocationObject;

                    long targetNodeId =
                            (long) invocation.get("targetNode");

                    String algorithm =
                            (String) invocation.get("algorithm");

                    statement.setString(1, graphName);
                    statement.setLong(2, sourceNodeId);
                    statement.setLong(3, targetNodeId);
                    statement.setString(4, algorithm);
                    statement.addBatch();

                    batchCounter++;

                    if (batchCounter == BATCH_SIZE) {
                        statement.executeBatch();
                        batchCounter = 0;
                    }
                }
            }

            if (batchCounter > 0) {
                statement.executeBatch();
            }
        }
    }

    private void createForeignKeys(Connection connection)
            throws SQLException {

        if (!foreignKeyExists(
                connection,
                "edges",
                "fk_graphName_nodeId"
        )) {
            String foreignKeySource =
                    "ALTER TABLE edges " +
                            "ADD CONSTRAINT fk_graphName_nodeId " +
                            "FOREIGN KEY (graphName, nodeId) " +
                            "REFERENCES nodes (graphName, nodeId)";

            try (Statement statement =
                         connection.createStatement()) {
                statement.execute(foreignKeySource);
            }
        }

        if (!foreignKeyExists(
                connection,
                "edges",
                "fk_graphName_targetNodeId"
        )) {
            String foreignKeyTarget =
                    "ALTER TABLE edges " +
                            "ADD CONSTRAINT fk_graphName_targetNodeId " +
                            "FOREIGN KEY (graphName, targetNodeId) " +
                            "REFERENCES nodes (graphName, nodeId)";

            try (Statement statement =
                         connection.createStatement()) {
                statement.execute(foreignKeyTarget);
            }
        }
    }

    private boolean foreignKeyExists(
            Connection connection,
            String tableName,
            String foreignKeyName
    ) throws SQLException {

        DatabaseMetaData metadata = connection.getMetaData();

        try (ResultSet resultSet =
                     metadata.getImportedKeys(
                             connection.getCatalog(),
                             null,
                             tableName
                     )) {

            while (resultSet.next()) {
                String existingForeignKey =
                        resultSet.getString("FK_NAME");

                if (foreignKeyName.equalsIgnoreCase(
                        existingForeignKey
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void main(String[] args) {
        MySqlCallGraphRepository repository =
                new MySqlCallGraphRepository();

        repository.importCallGraphs(
                new File("D:\\BA\\neuCG")
        );
    }
}