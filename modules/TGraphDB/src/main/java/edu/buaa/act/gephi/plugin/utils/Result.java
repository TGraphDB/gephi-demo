package edu.buaa.act.gephi.plugin.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by song on 16-5-28.
 */
public class Result{
    private Map<String,Long> map = new HashMap<String,Long>();
    public void inc(String key){
        Long i = map.get(key);
        if(i==null){
            map.put(key,1L);
        }else{
            map.put(key,i+1);
        }
    }
    public Long get(String key){
        Long i = map.get(key);
        if(i==null){
            throw new RuntimeException("TGraph: not such key in result.");
        }else{
            return i;
        }
    }
}
