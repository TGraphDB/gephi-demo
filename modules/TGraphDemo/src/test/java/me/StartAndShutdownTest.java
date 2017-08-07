package me;

import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.junit.After;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.lang.management.ManagementFactory;

/**
 * Created by song on 16-8-8.
 */
public class StartAndShutdownTest
{
    GraphDatabaseService db;

    @Test
    public void run() throws InterruptedException {
        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        Thread.sleep(20000);
        db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder("/tmp/amitabha")
                .loadPropertiesFromFile("")
                .newGraphDatabase();
        System.out.println("数据库初始化完成");
        Thread.sleep(100000);
//        new TransactionWrapper<Long>() {
//            @Override
//            public void runInTransaction() {
//                //随便换个id，72863,76561都没错。
//                Relationship r = db.getRelationshipById(74145);//error
////                Relationship r = db.getRelationshipById(72863);//随便换个id，72863,76561都没错。
////                Object o = r.getDynPropertyPointValue("travel-time", 1288833120);//
//                Object o = r.hasProperty("full-status");//
////                Object o = r.getDynPropertyPointValue("full-status", 1288818724);// error
//                System.out.println(o);
//            }
//        }.start(db);

    }

    @After
    public void finish() throws InterruptedException {
        if(db!=null) db.shutdown();Thread.sleep(100000);
    }

}
