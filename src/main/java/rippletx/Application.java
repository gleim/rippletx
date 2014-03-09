package rippletx;

import java.io.File;
import java.io.BufferedReader;

import java.util.logging.Logger;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Application {

    private static final String DB_PATH = "db/rippletxneo4j.db";

    private enum AccountRelationshipTypes implements RelationshipType
    {
        TRANSACTS_WITH
    }

    private static GraphDatabaseService graphDb;

    public static void run(String... args) throws Exception {

        // create property graph database
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );

        // register shutdown hook to prevent database corruption upon interruption
        registerShutdownHook( graphDb );

        try ( Transaction tx = graphDb.beginTx() ) {
            // add uniqueness constraint
            graphDb.schema()
                            .constraintFor( DynamicLabel.label("Account"))
                            .assertPropertyIsUnique("address")
                            .create();

            // commit schema constraint
            tx.success();
        }

        try ( Transaction tx = graphDb.beginTx() ) {

            UniqueFactory<Node> accountFactory = new UniqueFactory.UniqueNodeFactory( graphDb, "accounts")
            {
                @Override
                protected void initialize( Node created, Map<String, Object> properties )
                {
                    created.addLabel( DynamicLabel.label( "Account" ) );
                    created.setProperty( "address", properties.get( "address" ) );
                }                  
            };
            int records = 0;
    
            Collection<File> files = getFolderContents("./json");

            for (Iterator<File> iter = files.iterator(); iter.hasNext();)
            {
                File file = iter.next();
                String filename = file.getName();

                System.out.println("Processing " + filename);

                String s = FileUtils.readFileToString(file);

                JSONObject o = (JSONObject)new JSONTokener(s).nextValue();
                {
                    String[] names = JSONObject.getNames(o);

                    for (String name : names) {

                        if (o.get(name) instanceof JSONObject) {

                            JSONObject result = (JSONObject)o.get(name);

                            JSONArray txs = (JSONArray)result.get("txs");
                            for (int i = 0; i < txs.length(); i++) {
                                JSONObject internal_tx = (JSONObject)txs.get(i);
                                String[] tx_names = JSONObject.getNames(internal_tx);

                                String input_address = "", output_address = "";

                                for (String tx_name : tx_names) {

                                    if (tx_name.equals("Account") || (tx_name.equals("Destination"))) {
                                        //System.out.println(tx_name + " : " + internal_tx.getString(tx_name));                                            

                                        if (tx_name.equals("Account")) {
                                            input_address = internal_tx.getString(tx_name);
                                        }

                                        if (tx_name.equals("Destination")) {
                                            output_address = internal_tx.getString(tx_name);
                                        }
                                    }
                                }

                                if (input_address != null && !input_address.isEmpty() && output_address != null && !output_address.isEmpty()) {

                                    // create property graph representation of entity associated with transaction input
                                    Node inputNode = accountFactory.getOrCreate( "address", input_address );

                                    // create property graph representation of entity associated with transaction output
                                    Node outputNode = accountFactory.getOrCreate( "address", output_address );
                         
                                    // create property graph representation of transaction relationship
                                    Relationship relationship = inputNode.createRelationshipTo( outputNode, AccountRelationshipTypes.TRANSACTS_WITH );

                                    // create details of transaction relationship
                                    //relationship.setProperty( "timestamp", timestamp );

                                    System.out.println(input_address + " transacts with " + output_address);
                                }
                            }
                        }
                    }
                }
            }
            // commit the transactions to the database
            tx.success();
        } 
    }

    /**
     * Returns all the files in a directory.
     * 
     * @param dir
     *            - Path to the directory that contains the text documents to be
     *            parsed.
     * @return A collection of File Objects
     */
    public static Collection<File> getFolderContents(String dir)
    {
        // Collect all readable documents
        File file = new File(dir);
        Collection<File> files = FileUtils.listFiles(file, CanReadFileFilter.CAN_READ, DirectoryFileFilter.DIRECTORY);
        return files;
    }

    public static void main(String[] args) throws Exception {
        org.neo4j.kernel.impl.util.FileUtils.deleteRecursively(new File( DB_PATH ));

        Application.run(args);
    }

    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }
}
