package edu.buaa.act.gephi.plugin.task;

import org.act.tgraph.demo.utils.Helper;
import org.act.tgraph.demo.utils.TransactionWrapper;
import org.gephi.utils.longtask.spi.LongTask;

import java.util.Calendar;
import java.util.List;

/**
 * Created by song on 17-5-15.
 */
public abstract class Traverse extends TransactionWrapper<Object> implements LongTask, Runnable
{
    public GUICallBack callback;

    public Traverse(boolean commit)
    {
        super(commit);
    }


    public static String timePeriod2Str(int timePeriodSecond) {
        int t = timePeriodSecond;
        if(t<60){
            return t+" seconds";
        }else if(t<600){
            return t/60 +" minute "+ t%60+" seconds";
        }else if(t<3600){
            return t/60 +" minute";
        }else if((t % 3600)/60> 10){
            return t/3600+" hours "+(t%3600)/60+" minute";
        }else{
            return t/3600+" hours";
        }
    }

    public static String timestamp2String(final int timestamp){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(((long) timestamp) * 1000);
        String result = c.get(Calendar.YEAR)+"-"+(c.get(Calendar.MONTH)+1)+"-"+c.get(Calendar.DAY_OF_MONTH)+" "+
                c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE);
        return result;
    }

    public static String pathLength2Str(int len) {
        if(len<2000){
            return len+" m";
        }else{
            return String.format("%.1f km",len/1000f);
        }
    }

    public interface GUICallBack{
        void onResult(long searchNodeCount, List<Long> path, List<Integer> arriveTimes, int pathRealLength);
    }
}
