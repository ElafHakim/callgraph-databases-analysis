package de.callgraph.database.mongodb;

import com.mongodb.ExplainVerbosity;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.GraphLookupOptions;
import com.mongodb.client.model.Projections;

import de.callgraph.benchmark.CsvFileOutput;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static java.util.Arrays.asList;

public class MongoDbCallGraphQueries {

    private static MongoClient mongoClient;
    private static MongoCollection<Document> collection;

    public static void main(String[] args) {
        if (mongoClient == null) {
            try {
                mongoClient = MongoClients.create(
                        "mongodb://ls5vs016.cs.tu-dortmund.de:9045"
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

        CsvFileOutput csvFileOutput =
                new CsvFileOutput("mongo_queries");

        // Hier folgen deine bisherigen Abfragen Q1 bis Q8.
    }


        String Q1 = "Q1";
        //Q1 finde im Graph  maven-core-3.8.5-graph.json die Methode getRepositorySession, die in der Klasse
        //org/apache/maven/execution/MavenSession deklariert wurde
        String durationMs_Q1 =
                collection.find(and(
                                eq("name", "getRepositorySession"),
                                eq("declaredClass", "org/apache/maven/execution/MavenSession"),
                                eq("graphName", "maven-core-3.8.5-graph.json")
                        )).explain(ExplainVerbosity.EXECUTION_STATS)
                        .entrySet().stream().toList().get(2).getValue().toString().
                        split("executionTimeMillis=")[1].substring(0, 1);
        System.out.println(durationMs_Q1);
        csvFileOutput.append(Q1, durationMs_Q1);
        // ...................................
        //............................
        //................................
        String Q2 = "Q2";
        //Q2- Finde alle Methoden, die die Methode createMapper indirekt aufrufen,über genau 2 kanten
        Document explanation2;
        explanation2 = collection.aggregate(
                asList(
                        match(and(eq("name", "set"),
                                eq("declaredClass", "java/util/BitSet"),
                                eq("graphName", "ant-1.10.12-graph.json"))),
                        graphLookup("Callgraphs", "$id", "id",
                                "invocations.targetId", "p_1",
                                new GraphLookupOptions().maxDepth(0).
                                        restrictSearchWithMatch(
                                                eq("graphName","ant-1.10.12-graph.json"))
                        )
                        //, unwind("$p_1")
                        , addFields(new Field("p_1", "$p_1"))
                        ,graphLookup("Callgraphs", "$p_1.id", "id",
                                "invocations.targetId", "p_2",
                                new GraphLookupOptions().maxDepth(0).
                                        restrictSearchWithMatch(
                                                eq("graphName","ant-1.10.12-graph.json"))
                        )
                        //, unwind("$p-2")
                        , addFields(new Field("p_2", "$p_2"))
                        ,graphLookup("Callgraphs", "$p_2.id", "id",
                                "invocations.targetId", "p_3",
                                new GraphLookupOptions().maxDepth(0).
                                        restrictSearchWithMatch(
                                                eq("graphName","ant-1.10.12-graph.json"))
                        )
                        //, unwind("$p_1")
                        , addFields(new Field("p_3", "$p_3"))
                        , project(Projections.computed("p_3 ", "$p_3"))
                        , limit(5)

                )
        ).allowDiskUse(true).explain(ExplainVerbosity.EXECUTION_STATS);

        List<Document> stages2 = explanation2.get("stages", List.class);

        for (Document stage : stages2) {
            Document cursorStage = stage.get("$cursor", Document.class);

            if (cursorStage != null) {
                List<String> keys2 = Arrays.asList("executionStats", "executionTimeMillis");

                long durationMs_Q2 = cursorStage.getEmbedded(keys2, Integer.class);
                System.out.println(durationMs_Q2);
                csvFileOutput.append(Q2, String.valueOf(durationMs_Q2));
            }
        }

        // ...................................
        //............................
        //................................
        String Q3 = "Q3";
        //Q3- Finde alle Methoden, die eine Methode mit Name set indirekt aufrufen,über max 3 kanten
  /*      Bson filter1 = Filters.and (
                eq("name", "set"),
                eq("declaredClass", "java/util/BitSet"),
                eq("graphName", "ant-1.10.12-graph.json")
        );*/

        Document explanation3 = collection.aggregate(
                asList(
                        match(and(eq("name", "set"),
                                eq("declaredClass", "java/util/BitSet"),
                                eq("graphName", "ant-1.10.12-graph.json"))),
                        graphLookup("Callgraphs", "$id", "id",
                                "invocations.targetId", "ancestors",
                                new GraphLookupOptions().maxDepth(2).
                                        restrictSearchWithMatch(
                                                eq("graphName","ant-1.10.12-graph.json"))
                        )
                        , unwind("$ancestors")
                        , project(Projections.computed("name ", "$ancestors.name"))
                        , limit(10)
                )
        ).allowDiskUse(true).explain(ExplainVerbosity.EXECUTION_STATS);


        List<Document> stages_3 = explanation3.get("stages", List.class); // im stages gibt es cursor stage and graphLookup stage

        for (Document stage : stages_3) {
            Document cursorStage = stage.get("$cursor", Document.class);

            if (cursorStage != null) {
                List<String> keys2 = Arrays.asList("executionStats", "executionTimeMillis");
                long durationMs_Q3 = cursorStage.getEmbedded(keys2, Integer.class);
                System.out.println(durationMs_Q3);
                csvFileOutput.append(Q3, String.valueOf(durationMs_Q3));


            }
        }

        // ...................................
        //............................
        //................................


        String Q4 = "Q4";
        //Q4-Finde Methoden, die keine anderen Methoden aufrufen
        String durationMs_Q4 = collection.find(and(
                        eq("graphName", "clojure-1.11.1-graph.json"),
                        exists("invocations", true),
                        type("invocations", "array"),
                        size("invocations", 0))
                )
                .explain(ExplainVerbosity.EXECUTION_STATS)
                .entrySet().stream().toList().get(2).getValue().toString().
                split("executionTimeMillis=")[1].substring(0, 1);
        System.out.println(" Q4  "+durationMs_Q4);
        csvFileOutput.append(Q4, durationMs_Q4);
        // ...................................
        //............................
        //................................

        String Q5 = "Q5";
        //Q5-Finde im Graph jackson-core-2.13.2  Methoden, die
        // von keinen anderen Methoden aufgerufen werden
        Document explanation5 = collection.aggregate(
                asList(
                        graphLookup("Callgraphs", "$id", "id",
                                "invocations.targetId", "result",// index on connectToField
                                new GraphLookupOptions().maxDepth(0).
                                        restrictSearchWithMatch(eq("graphName","jackson-core-2.13.2-graph.json"))
                        )
                        , match(and(
                                exists("result", true),
                                type("result", "array"),
                                size("result", 0)))
                        , project(Projections.computed("name ", "result.name"))
                        , limit(1)
                )
        ).explain(ExplainVerbosity.EXECUTION_STATS);

        List<Document> stages5 = explanation5.get("stages", List.class);// im stages gibt es cursorStage and graphLookupStage

        for (Document stage : stages5) {
            Document cursorStage = stage.get("$cursor", Document.class);

            if (cursorStage != null) {
                List<String> keys2 = Arrays.asList("executionStats", "executionTimeMillis");

                long durationMs_Q5 = cursorStage.getEmbedded(keys2, Integer.class);
                System.out.println( "Q5 "+ durationMs_Q5);
                csvFileOutput.append(Q5, String.valueOf(durationMs_Q5));
            }
        }
        // ...................................
        //............................
        //................................


        String Q6 = "Q6";
        //Q6-Finde im Graph Methoden, die zwei Methoden m1 und m2 im gleichen Graph aufrufen
        //"id":491,"name":"setUsageWidth","descriptor":"(I)V","declaredClass":"org/kohsuke/args4j/CmdLineParser"
        // "id":86,"name":"put","descriptor":"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;","paramCnt":2,"declaredClass":"java/util/Map"

        String durationMs_m1 =
                collection.find(and(
                                        eq("name", "put"),
                                        eq("declaredClass", "java/util/Map"),
                                        eq("graphName", "args4j-2.33-graph.json")
                                )
                        ).
                        explain(ExplainVerbosity.EXECUTION_STATS)
                        .entrySet().stream().toList().get(2).getValue().toString().
                        split("executionTimeMillis=")[1].substring(0, 1);
        long m1_s = System.currentTimeMillis();
        Iterator m1 = collection.find(
                and(
                        eq("name", "put"),
                        eq("declaredClass", "java/util/Map"),
                        eq("graphName", "args4j-2.33-graph.json")
                )
        ).iterator();
        Document document = new Document();
        while (m1.hasNext()) {
            document = (Document) m1.next();
        }
        long id_m1 = (long) document.values().toArray()[1];
        long m1_d = System.currentTimeMillis() - m1_s;

        String durationMs_m2 =
                collection.find(and(
                                        eq("name", "setUsageWidth"),
                                        eq("declaredClass", "org/kohsuke/args4j/CmdLineParser"),
                                        eq("graphName", "args4j-2.33-graph.json")
                                )
                        ).
                        explain(ExplainVerbosity.EXECUTION_STATS)
                        .entrySet().stream().toList().get(2).getValue().toString().
                        split("executionTimeMillis=")[1].substring(0, 1);
        long m2_s = System.currentTimeMillis();
        Iterator m2 = collection.find(
                and(
                        eq("name", "setUsageWidth"),
                        eq("declaredClass", "org/kohsuke/args4j/CmdLineParser"),
                        eq("graphName", "args4j-2.33-graph.json")
                )
        ).iterator();
        document = new Document();
        while (m2.hasNext()) {
            document = (Document) m2.next();
        }
        long id_m2 = (long) document.values().toArray()[1];
        long m2_d = System.currentTimeMillis() - m2_s;
        String durationMs6 =
                collection.find(and(
                                eq("invocations.targetId", id_m1),
                                eq("invocations.targetId", id_m2))
                        ).
                        explain(ExplainVerbosity.EXECUTION_STATS)
                        .entrySet().stream().toList().get(2).getValue().toString().
                        split("executionTimeMillis=")[1].substring(0, 1);
        System.out.println(" m1_d "  + m1_d);
        System.out.println(" m2_d "  + m2_d);
        System.out.println("m1 "+ durationMs_m1 );
        System.out.println("m2 "+ durationMs_m2);
        System.out.println( "Q6  "+durationMs6 );
        csvFileOutput.append(Q6, durationMs6);
        // ...................................
        //............................
        //................................
        String Q7 = "Q7";
        //Q7-Wie viele Relationen sind mit dem Algorithmus cha generiert wurden.
        Document explanation7 = collection.aggregate(
                asList(
                        match(elemMatch("invocations", eq("algorithm", "cha")))
                        , project(Projections.computed("_id",
                                        new Document().append("id", "$id").append("name", "$name").append("invocations", "$invocation")
                                )
                        )
                        , project(Projections.computed("invocations", "1"))
                        , unwind("$invocations")  //Der $unwind Befehl erstellt ein Dokument für jeden Eintrag im Array field
                        , match(eq("invocations.alogorithm", "cha"))
                        , group("$id", Accumulators.sum("countCha", 1))
                        //group("$id", Accumulators.sum("countCha", size())),
                        , project(Projections.computed("countCha", "$countCha"))
                )
        ).explain(ExplainVerbosity.EXECUTION_STATS);

        List<Document> stages7 = explanation7.get("stages", List.class); // im stages gibt es cursor stage and graphLookup stage

        for (Document stage : stages7) {
            Document cursorStage = stage.get("$cursor", Document.class);

            if (cursorStage != null) {
                List<String> keys2 = Arrays.asList("executionStats", "executionTimeMillis");
                //System.out.println(cursorStage.getEmbedded(keys2, Integer.class));
                long durationMs_7 = cursorStage.getEmbedded(keys2, Integer.class);
                System.out.println(durationMs_7);
                csvFileOutput.append(Q7, String.valueOf(durationMs_7));
            }
        }


        //................................
        //...................
        //.......................
        String Q8 = "Q8";
        //Q8- Wie viele Methoden rufen im Graph jackson-databind-2.13.2.2-graph.json die Methode findEnum, die in der
        //klasse "" deklariert wurde
/*        Document explanation8 = collection.aggregate(asList(
                match(and(eq("name", "_findCustomBeanDeserializer")
                        ,eq("declaredClass", "com/fasterxml/jackson/databind/deser/BasicDeserializerFactory")
                        ,eq("graphName", "jackson-databind-2.13.2.2-graph.json"))),
                graphLookup("Callgraphs", "$id", "id",
                        "invocations.targetId", "Caller_findEnum",
                        new GraphLookupOptions().maxDepth(0).
                                restrictSearchWithMatch(
                                        eq("graphName","jackson-databind-2.13.2.2-graph.json"))
                ) //{"$project":{"total_ancenstors":{"$size":"ancestors"}}}
                //, unwind("$ancestors")
                , unwind("$Caller_findEnum")
                //,match(eq("graphName", "jackson-databind-2.13.2.2-graph.json"))
                , group("Caller_findEnum", Accumulators.sum("count", 1))
                , project(Projections.computed("count", "1"))
                , size("Caller_findEnum ", 10)

        )).explain(ExplainVerbosity.EXECUTION_STATS);*/
        Bson match = match(and(eq("name", "_findCustomBeanDeserializer")
                ,eq("declaredClass", "com/fasterxml/jackson/databind/deser/BasicDeserializerFactory")
                ,eq("graphName", "jackson-databind-2.13.2.2-graph.json")));
        Bson graphLookup = graphLookup("Callgraphs", "$id", "id",
                "invocations.targetId", "Caller_findEnum",
                new GraphLookupOptions().maxDepth(0).
                        restrictSearchWithMatch(
                                eq("graphName","jackson-databind-2.13.2.2-graph.json"))
        );
        Bson project = project(new Document("count", new Document("$size", "$Caller_findEnum" )));
        Document explanation8 = collection.aggregate(Arrays.asList(match, graphLookup,  project)).explain(ExplainVerbosity.EXECUTION_STATS);

        List<Document> stages_8 = explanation8.get("stages", List.class); // im stages gibt es cursor stage and graphLookup stage

        for (Document stage : stages_8) {
            Document cursorStage = stage.get("$cursor", Document.class);

            if (cursorStage != null) {
                List<String> keys2 = Arrays.asList("executionStats", "executionTimeMillis");
                long durationMs_Q8 = cursorStage.getEmbedded(keys2, Integer.class);
                System.out.println(durationMs_Q8);
                csvFileOutput.append(Q8, String.valueOf(durationMs_Q8));

            }
        }


    }
