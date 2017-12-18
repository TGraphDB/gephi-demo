package edu.buaa.act.gephi.plugin.task;

import com.graphhopper.reader.ReaderElement;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.osm.OSMInputFile;
import edu.buaa.act.gephi.plugin.utils.GUIHook;
import edu.buaa.act.gephi.plugin.utils.OSMStorage;
import gnu.trove.list.TLongList;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;

import java.io.File;

/**
 * Created by song on 17-12-17.
 */
public abstract class LoadOSMDataAsyncTask extends GUIHook<OSMStorage> implements LongTask, Runnable  {

    private final File file;
    private ProgressTicket progress;
    private OSMStorage storage;

    public LoadOSMDataAsyncTask(File osmFile){
        this.file = osmFile;
    }

    @Override
    public void run() {
        Progress.switchToIndeterminate(this.progress);
        Progress.start(this.progress);
        storage = new OSMStorage(this.file);
        storage.loadFile();
        Progress.finish(this.progress);
        this.handler(this.storage);
    }

    @Override
    public boolean cancel() {
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }
}
