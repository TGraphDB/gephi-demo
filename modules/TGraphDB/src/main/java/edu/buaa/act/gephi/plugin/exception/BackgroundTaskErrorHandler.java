package edu.buaa.act.gephi.plugin.exception;

import org.gephi.utils.longtask.api.LongTaskErrorHandler;

/**
 * Created by song on 16-5-29.
 */
public class BackgroundTaskErrorHandler implements LongTaskErrorHandler {
    private static BackgroundTaskErrorHandler self;
    public static BackgroundTaskErrorHandler instance(){
        if (self==null){
            self = new BackgroundTaskErrorHandler();
        }
        return self;
    }
    private BackgroundTaskErrorHandler(){}


    @Override
    public void fatalError(Throwable throwable) {
        System.out.println("=============== error in task ==============");
        throwable.printStackTrace(System.out);
        System.out.println("============================================");
    }
}
