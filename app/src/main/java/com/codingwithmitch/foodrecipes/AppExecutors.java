package com.codingwithmitch.foodrecipes;


import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AppExecutors {

    // singleton
    private static AppExecutors instance;
    public static AppExecutors getInstance(){
        if(instance == null){
            instance = new AppExecutors();
        }
        return instance;
    }

    // single background thread
    private final Executor mDiskIO = Executors.newSingleThreadExecutor();

    // main thread
    private final Executor mMainThreadExecutor = new MainThreadExecutor();

    // getters
    public Executor diskIO(){ return mDiskIO; }
    public Executor mainThread(){ return mMainThreadExecutor; }
    
    private static class MainThreadExecutor implements Executor {

        // get handle to main thread
        private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        // execute on main thread
        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
