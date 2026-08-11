package de.callgraph.database.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;

import de.callgraph.benchmark.CsvFileOutput;

import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MongoDbCallGraphRepository {

    private final MongoCollection<Document> collection;

    public MongoDbCallGraphRepository() {
        this.collection = MongoDbConnection.getCollection();
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
                new CsvFileOutput("mongodb_laufzeit_clojure");

        JSONParser parser = new JSONParser();

        System.out.println("------ Start ------");

        for (File callGraphFile : files) {
            importCallGraph(callGraphFile, parser, csvFileOutput);
        }

        createIndexes();
    }

    private void importCallGraph(
            File callGraphFile,
            JSONParser parser,
            CsvFileOutput csvFileOutput
    ) {
        try (FileReader reader = new FileReader(callGraphFile)) {
            JSONArray methods =
                    (JSONArray) parser.parse(reader);

            String graphName = callGraphFile.getName();
            long startTime = System.currentTimeMillis();

            for (Object methodObject : methods) {
                JSONObject method = (JSONObject) methodObject;
                Document node = createNodeDocument(method, graphName);

                collection.insertOne(node);
            }

            long durationSeconds =
                    (System.currentTimeMillis() - startTime) / 1000;

            System.out.println(
                    graphName + " " + durationSeconds + " Sekunden"
            );

            csvFileOutput.append(
                    graphName,
                    String.valueOf(durationSeconds)
            );

        } catch (IOException | ParseException e) {
            throw new IllegalStateException(
                    "Callgraph-Datei konnte nicht gelesen werden: "
                            + callGraphFile.getAbsolutePath(),
                    e
            );
        }
    }

    private Document createNodeDocument(
            JSONObject method,
            String graphName
    ) {
        long id = (long) method.get("id");
        String name = (String) method.get("name");
        String descriptor = (String) method.get("descriptor");
        long paramCnt = (long) method.get("paramCnt");
        String declaredClass =
                (String) method.get("declaredClass");

        JSONArray invocations =
                (JSONArray) method.get("invocations");

        List<BasicDBObject> invocationDocuments =
                new ArrayList<>();

        for (Object invocationObject : invocations) {
            JSONObject invocation =
                    (JSONObject) invocationObject;

            String algorithm =
                    (String) invocation.get("algorithm");

            long targetNode =
                    (long) invocation.get("targetNode");

            BasicDBObject invocationDocument =
                    new BasicDBObject()
                            .append("algorithm", algorithm)
                            .append("targetId", targetNode);

            invocationDocuments.add(invocationDocument);
        }

        return new Document("id", id)
                .append("name", name)
                .append("graphName", graphName)
                .append("descriptor", descriptor)
                .append("paramCnt", paramCnt)
                .append("declaredClass", declaredClass)
                .append("invocations", invocationDocuments);
    }

    private void createIndexes() {
        try {
            collection.createIndex(
                    Indexes.ascending(
                            "name",
                            "declaredClass",
                            "graphName"
                    )
            );

            collection.createIndex(
                    Indexes.ascending("invocations")
            );

            collection.createIndex(
                    Indexes.ascending(
                            "invocations.targetId",
                            "invocations.algorithm"
                    )
            );

            collection.createIndex(
                    Indexes.ascending("invocations.targetId")
            );

            collection.createIndex(
                    Indexes.ascending("invocations.algorithm")
            );

            collection.createIndex(
                    Indexes.ascending("name", "graphName")
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "MongoDB-Indizes konnten nicht erstellt werden.",
                    e
            );
        }
    }

    public static void main(String[] args) {
        MongoDbCallGraphRepository repository =
                new MongoDbCallGraphRepository();

        try {
            repository.importCallGraphs(
                    new File("D:\\BA\\clojure")
            );
        } finally {
            MongoDbConnection.closeConnection();
        }
    }
}