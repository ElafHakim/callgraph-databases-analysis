package CallGraphApp;

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

public class Neo4j implements AutoCloseable {
    @Override
    public void close() {
    }
    public static Driver neo4jDriver;
    public static void createDriver() {
        try {
            neo4jDriver = GraphDatabase.driver("bolt://ls5vs016.cs.tu-dortmund.de:9008",  // 9778 connection failed nach 4 graphs
                    AuthTokens.basic("neo4j", "neo5j"));
            neo4jDriver.verifyConnectivity();
        } catch (Exception e) {
            neo4jDriver.close();//Closing a driver immediately shuts down all open connections.
            throw new IllegalStateException("Cannot create neo4jDriver  !", e);
        }
    }

    static String graphName;
    static JSONArray jsonArr;

    public static void main(String... args) throws Exception {

        JSONObject method ;
        long id ;
        String name ;
        String descriptor;
        long paramCnt ;
        String declaredClass ;
        createDriver();
        CsvFileOutput csvFileOutput = new CsvFileOutput("neo4j-lauftest2-clojure");
        JSONParser parser = new JSONParser();
        try {
            File dir = new File("D:\\BA\\neuCG");
            File[] directoryListing = dir.listFiles();
            assert directoryListing != null;
            long durationOf100Graphs = 0;
            System.out.println("-----start ------");
            //for (int k = 0; k < 5; k++) {

           /*for (int f = 0; f < directoryListing.length; f++) {
                File cgFile = directoryListing[f];*/

            for (File cgFile : directoryListing) { // ein Callgraph

                graphName = cgFile.getName();
                jsonArr = (JSONArray) parser.parse(new FileReader(cgFile));
                // long durationMsForNode, durationMsForEdge;
                //long durationTotal = 0;
                //long startTimeForNode = System.currentTimeMillis();

                long startTime = System.currentTimeMillis();
                try (Session sessionForNodes = neo4jDriver.session()) {

                    for (int i = 0; i < jsonArr.size(); i++) {

                         method = (JSONObject) jsonArr.get(i);
                         id = (long) method.get("id");
                         name = (String) method.get("name");
                         descriptor = (String) method.get("descriptor");
                         paramCnt = (long) method.get("paramCnt");
                         declaredClass = (String) method.get("declaredClass");
                        sessionForNodes.run("CREATE (n:Method {uid :$uid,  graphName: $graphName, name: $name, id: $id, descriptor:$descriptor, paramCnt: $paramCnt, declaredClass: $declaredClass })",
                                parameters("uid", graphName + id, "graphName", graphName, "name", name, "id", id, "descriptor", descriptor, "paramCnt", paramCnt, "declaredClass", declaredClass));

                        // durationMsForNode = System.currentTimeMillis() - startTimeForNode;
                        //durationMsTotal += durationMsForNode;
                    }// node

                    sessionForNodes.run("CREATE INDEX nodeIndex IF NOT EXISTS FOR (n:Method) ON (n.graphName, n.name, n.declaredClass ) ");
                    sessionForNodes.run("CREATE INDEX graphNameIndex IF NOT EXISTS FOR (n:Method) ON (n.graphName ) ");
                    sessionForNodes.run("CREATE CONSTRAINT uniqueConstraint IF NOT EXISTS FOR (n:Method) REQUIRE n.uid IS UNIQUE ");
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot create Session to create nodes!", e);
                }

                JSONObject method1;
                long id2;
                JSONArray invocations;
                JSONObject invObj ;
                String algorithm ;
                long targetNode;
                //long startTimeForEdge = System.currentTimeMillis();
                try (Session sessionForEdges = neo4jDriver.session()) {

                    for (int i = 0; i < jsonArr.size(); i++) {
                         method1 = (JSONObject) jsonArr.get(i);
                         id2 = (long) method1.get("id");
                         invocations = (JSONArray) method1.get("invocations");
                        if (invocations.size() != 0) {
                            for (int j = 0; j < invocations.size(); j++) {
                                 invObj = (JSONObject) invocations.get(j);
                                 algorithm = (String) invObj.get("algorithm");
                                 targetNode = (long) invObj.get("targetNode");

                                sessionForEdges.run("MATCH (m1:Method{uid : $uid }),(m2:Method{ uid: $targetUid })" +
                                                "CREATE (m1)-[:calls{algorithm: $algorithm}]->(m2)",
                                        parameters("uid", graphName + id2, "targetUid", graphName + targetNode, "algorithm", algorithm));

                            }// invocations loop
                        } // if
                    } // edge loop
                    sessionForEdges.run("CREATE INDEX algorithmIndex IF NOT EXISTS FOR ()-[r:calls]-() ON (r.algorithm) ");
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot create session to create edges!", e);
                }

                //durationMsForEdge = System.currentTimeMillis() - startTimeForEdge;
                //durationMsTotal += durationMsForEdge;

                long durationSeconds = (System.currentTimeMillis() - startTime) / 1000; // for nodes und edges
                csvFileOutput.append(cgFile.getName(), String.valueOf(durationSeconds));
                System.out.println(graphName + " " + durationSeconds + " seconds");
                durationOf100Graphs += durationSeconds;



            } // directoryListing
            TimeUtil.setValue(0, durationOf100Graphs);
            // }//k
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    } // main
}