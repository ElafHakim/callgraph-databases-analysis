package de.callgraph.database.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDbConnection {

    private static MongoClient mongoClient;
    private static MongoCollection<Document> collection;

    private MongoDbConnection() {
        // Verhindert das Erzeugen von Objekten dieser Klasse
    }

    public static MongoCollection<Document> getCollection() {
        if (collection == null) {
            try {
                mongoClient = MongoClients.create(
                        "mongodb://ls5vs016.cs.tu-dortmund.de:9929"
                );

                MongoDatabase database =
                        mongoClient.getDatabase("myDB");

                collection =
                        database.getCollection("Callgraphs");

            } catch (Exception e) {
                throw new IllegalStateException(
                        "MongoDB-Verbindung konnte nicht hergestellt werden.",
                        e
                );
            }
        }

        return collection;
    }

    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            collection = null;
        }
    }
}