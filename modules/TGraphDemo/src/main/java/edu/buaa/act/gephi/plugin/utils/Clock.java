package edu.buaa.act.gephi.plugin.utils;

import org.act.neo4j.temporal.demo.utils.Helper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by song on 16-6-6.
 */
public class Clock {
    private List<Long> times = new ArrayList<Long>();
    private List<String> content = new ArrayList<String>();
    public void lap(String s){
        times.add(System.currentTimeMillis());
        content.add(s);
    }

    public void start(String s) {
        lap(s);
    }

    public void stop() {
        lap("AMTB:STOP");
        System.out.println("============= time usage ===========");
        for(int i=1;i<times.size();i++){
            System.out.println(content.get(i-1)+" "+timePeriod2Str((int) (times.get(i)-times.get(i-1))));
        }
        System.out.println("====================================");
        times.clear();
        content.clear();
    }

    private String timePeriod2Str(int timePeriodMilliSecond){
        int t = timePeriodMilliSecond/1000;
        if(t<1){
            return timePeriodMilliSecond+" millis seconds";
        }else if(t<120) {
            return t + " seconds";
        }else if(t<600) {
            return t/60 +" minute "+ t%60+" seconds";
        }else{
            return t/60 +" minute";
        }
    }
}
