package de.callgraph.database.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class MySqlConnection {

    private static final String URL =
            "jdbc:mysql://ls5vs016.cs.tu-dortmund.de:9366/callgraphs";

    private static final String USER = "root";
    private static final String PASSWORD = "mypassword";

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

        } catch (ClassNotFoundException | SQLException e) {
            throw new IllegalStateException(
                    "Die MySQL-Verbindung konnte nicht hergestellt werden.",
                    e
            );
        }
    }
}