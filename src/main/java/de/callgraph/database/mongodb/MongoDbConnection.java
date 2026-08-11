package de.callgraph.database.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;

public final class MongoDbConnection {

    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private static final String URI =
            getRequiredValue("MONGODB_URI");

    private static final String DATABASE_NAME =
            getRequiredValue("MONGODB_DATABASE");

    private static final String COLLECTION_NAME =
            getRequiredValue("MONGODB_COLLECTION");

    private static MongoClient mongoClient;
    private static MongoCollection<Document> collection;

    private MongoDbConnection() {
        // Verhindert das Erzeugen von Objekten dieser Klasse
    }

    public static synchronized MongoCollection<Document> getCollection() {
        if (collection == null) {
            try {
                mongoClient = MongoClients.create(URI);

                MongoDatabase database =
                        mongoClient.getDatabase(DATABASE_NAME);

                collection =
                        database.getCollection(COLLECTION_NAME);

            } catch (Exception e) {
                closeConnection();

                throw new IllegalStateException(
                        "Die MongoDB-Verbindung konnte nicht hergestellt werden.",
                        e
                );
            }
        }

        return collection;
    }

    public static synchronized void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            collection = null;
        }
    }

    private static String getRequiredValue(String variableName) {
        String value = DOTENV.get(variableName);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Die Umgebungsvariable " + variableName
                            + " wurde nicht festgelegt."
            );
        }

        return value;
    }
}