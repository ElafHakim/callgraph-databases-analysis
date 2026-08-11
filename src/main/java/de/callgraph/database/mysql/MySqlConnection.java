package de.callgraph.database.mysql;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class MySqlConnection {

    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private static final String URL = getRequiredValue("MYSQL_URL");
    private static final String USER = getRequiredValue("MYSQL_USER");
    private static final String PASSWORD = getRequiredValue("MYSQL_PASSWORD");

    private MySqlConnection() {
        // Verhindert das Erzeugen eines Objekts dieser Klasse
    }

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            return DriverManager.getConnection(
                    URL,
                    USER,
                    PASSWORD
            );

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Der MySQL-Treiber wurde nicht gefunden.",
                    e
            );

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Die MySQL-Verbindung konnte nicht hergestellt werden.",
                    e
            );
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