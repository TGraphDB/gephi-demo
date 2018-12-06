package edu.buaa.act.gephi.plugin.task;

import edu.buaa.act.gephi.plugin.utils.GUIHook;
import org.act.tgraph.demo.vo.RoadChain;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Read source files(Topo.csv) to RoadChain list.
 * Created by song on 16-5-27.
 */
public class ReadNetworkFileTask implements LongTask, Runnable {

    private final File networkFile;
    private ProgressTicket progress;

    public ReadNetworkFileTask(File networkFile){
        this.networkFile = networkFile;
    }
    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }

    @Override
    public void run() {
        Progress.start(progress, 130000);
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
                Progress.progress(progress, "reading "+ lineCount +" lines",1);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Progress.finish(progress);
        new GUIHook<List<RoadChain>>(){
            @Override
            public void guiHandler(List<RoadChain> value) {
                onResult(value);
            }
        }.guiHandler(roadChainList);
    }

    protected void onResult(List<RoadChain> list){

    }
}
