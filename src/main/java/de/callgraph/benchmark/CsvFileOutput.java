package de.callgraph.benchmark;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CsvFileOutput {

    private final String name;
    private final String outputDirectory = "results/csv";

    public CsvFileOutput(String name) {
        this.name = name;

        File directory = new File(outputDirectory);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (FileWriter writer =
                     new FileWriter(outputDirectory + "/" + name + ".csv", true)) {

            writer.append("GraphName,")
                  .append(name)
                  .append(System.lineSeparator());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void append(String path, String value) {
        try (FileWriter writer =
                     new FileWriter(outputDirectory + "/" + name + ".csv", true)) {

            writer.append(path)
                  .append(",")
                  .append(value)
                  .append(System.lineSeparator());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}