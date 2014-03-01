package rippletx;

import java.io.File;
import java.io.BufferedReader;

import java.util.logging.Logger;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;

import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

@Configuration
@EnableNeo4jRepositories
public class Application extends Neo4jConfiguration implements CommandLineRunner {

    private static final String DB_PATH = "db/rippletxneo4j.db";

    @Bean
    EmbeddedGraphDatabase graphDatabaseService() {
        return new EmbeddedGraphDatabase( DB_PATH );
    }

    @Autowired
    PersonRepository personRepository;

    @Autowired
    GraphDatabase graphDatabase;

    public void run(String... args) throws Exception {

        Transaction tx = graphDatabase.beginTx();
        try {

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

                                System.out.println("**********************************************");

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
                                    Person in  = new Person(input_address);
                                    Person out = new Person(output_address);
                                    in.transactsWith(out);
                                    personRepository.save(in);

                                    System.out.println(input_address + " transacts with " + output_address);
                                }
                            }
                        }
                    }
                }
            }
            tx.success();
        } finally {
            tx.finish();
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

        SpringApplication.run(Application.class, args);
    }

}
