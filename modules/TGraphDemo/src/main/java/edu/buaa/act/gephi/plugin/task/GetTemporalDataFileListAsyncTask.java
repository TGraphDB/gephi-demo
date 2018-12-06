package edu.buaa.act.gephi.plugin.task;

import com.google.common.collect.Lists;
import edu.buaa.act.gephi.plugin.utils.GUIHook;
import org.act.tgraph.demo.utils.Helper;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.ProgressTicket;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * get Traffic status file list
 * Created by song on 16-5-27.
 */
public class GetTemporalDataFileListAsyncTask implements LongTask, Runnable {

    private final File dataPath;

    public GetTemporalDataFileListAsyncTask(File dataFolder){
        this.dataPath = dataFolder;
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {

    }

    @Override
    public void run() {
        List<File> fileList = new ArrayList<File>();
        Helper.getFileRecursive(dataPath, fileList, 5);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        new GUIHook<List<File>>(){
            @Override
            public void guiHandler(List<File> value) {
                onResult(value);
            }
        }.guiHandler(fileList);
    }

    protected void onResult(List<File> value){

    }
}
