package me;

import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.junit.After;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Created by song on 16-7-28.
 */
public class DataCheckTest {
    GraphDatabaseService db;

    @Test
    public void run(){ //请学长调试这个程序_我已经把问题简化了
        db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder("/home/song/tmp/amitabha")
                .loadPropertiesFromFile("")
                .newGraphDatabase();
        System.out.println("数据库初始化完成");

        new TransactionWrapper<Long>() {
            @Override
            public void runInTransaction() {
                //随便换个id，72863,76561都没错。74145 error
                Relationship r = db.getRelationshipById(72053);//error
//                Relationship r = db.getRelationshipById(72863);//随便换个id，72863,76561都没错。
//                Object o = r.getDynPropertyPointValue("travel-time", 1288833120);//
                r.setTemporalProperty("test",0,1);
                Object o = r.hasProperty("travel-time");//
//                Object o = r.getDynPropertyPointValue("full-status", 1288818724);// error
                Object o1 = r.getTemporalProperty("travel-time", 1288800000);// error
                System.out.println(o);
                System.out.println(o1);
            }
        }.start(db);

    }

    @After
    public void finishi(){
        if(db!=null) db.shutdown();
    }
}
