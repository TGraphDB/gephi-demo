package me;

import edu.buaa.act.gephi.plugin.task.BuildDatabaseAsyncTask;
import org.act.neo4j.temporal.demo.utils.Helper;
import org.act.neo4j.temporal.demo.utils.TransactionWrapper;
import org.act.neo4j.temporal.demo.vo.RoadChain;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by song on 16-5-28.
 */
public class DataImportTest {
    private File networkFile = new File("/home/song/tmp/Topo.csv");
    private File dataPath = new File("/home/song/tmp/road data");
    private File dbDirectory = new File("/home/song/tmp/tgraph-test");
    private List<RoadChain> getRoadList(){
        final List<RoadChain> roadChainList = new ArrayList<RoadChain>();

        try{
            BufferedReader br = new BufferedReader(new FileReader(this.networkFile));
            String line;
            for (int lineCount = 0; (line = br.readLine()) != null; lineCount++) {
                if (lineCount == 0) continue;//ignore headers
                try {
                    roadChainList.add(new RoadChain(line, lineCount));
                }catch (RuntimeException e){
                    System.out.println(e.getMessage()+" at line:"+lineCount);
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return roadChainList;
    }
    private List<File> getDataFileList(){
        List<File> fileList = new ArrayList<File>();
        Helper.getFileRecursive(dataPath, fileList, 5);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return fileList;
    }
    @Test
    public void run(){
        Helper.deleteAllFilesOfDir(dbDirectory);
        dbDirectory.mkdir();
        BuildDatabaseAsyncTask task = new BuildDatabaseAsyncTask(
                getRoadList(),
                getDataFileList().subList(0,160),
                dbDirectory
        );
        task.setProgressTicket(new ProgressTicket() {
            @Override
            public void finish() {

            }

            @Override
            public void finish(String s) {

            }

            @Override
            public void progress() {

            }

            @Override
            public void progress(int i) {

            }

            @Override
            public void progress(String s) {

            }

            @Override
            public void progress(String s, int i) {
                System.out.println(s);
            }

            @Override
            public void setDisplayName(String s) {

            }

            @Override
            public String getDisplayName() {
                return null;
            }

            @Override
            public void start() {

            }

            @Override
            public void start(int i) {

            }

            @Override
            public void switchToDeterminate(int i) {

            }

            @Override
            public void switchToIndeterminate() {

            }
        });
        task.run();
        System.exit(0);
    }

    @Test
    public void find(){
        final GraphDatabaseService db = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(dbDirectory.getAbsolutePath())
                .loadPropertiesFromFile("")
                .newGraphDatabase();
        new TransactionWrapper<Object>(){

            @Override
            public void runInTransaction() {
                Relationship r = db.getRelationshipById(46388);
                System.out.println(r.getDynPropertyPointValue("travel-time", 1320400594));
            }
        }.start(db);
        db.shutdown();
    }
}
