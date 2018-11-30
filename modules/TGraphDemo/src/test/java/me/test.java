package play;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.ErrorState;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Created by song on 2018-06-11.
 */
public class test
{
    public static void deleteFile(File element) {
        if (element.isDirectory()) {
            for (File sub : element.listFiles()) {
                deleteFile(sub);
            }
        }
        element.delete();
    }

    public GraphDatabaseService db( boolean fromScratch ) throws IOException
    {
        File dir = new File( System.getProperty( "java.io.tmpdir" ), "TGRAPH-db" );
        if ( fromScratch )
        {
            deleteFile( dir );
        }
        return new GraphDatabaseFactory().newEmbeddedDatabase( dir );
    }

    @Test
    public void createIndexes() throws IOException
    {
        GraphDatabaseService neo4j = db( true );
        Node node;
        try ( Transaction tx = neo4j.beginTx() )
        {
            node = neo4j.createNode();
            node.setProperty( "hehe", "haha" );
            node.setProperty( "hehe", 1 );
            node.setProperty( "hehe", 2 );
            tx.success();
        }

        try ( Transaction tx = neo4j.beginTx() )
        {
            node = neo4j.getNodeById( node.getId() );
            node.setProperty( "hehe", "haha" );
            node.setProperty( "hehe", 1 );
            node.setProperty( "hehe", 2 );
            tx.success();
        }
        neo4j.shutdown();
    }


}
