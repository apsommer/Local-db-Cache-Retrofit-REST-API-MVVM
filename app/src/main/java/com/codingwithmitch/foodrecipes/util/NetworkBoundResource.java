package com.codingwithmitch.foodrecipes.util;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;

/**
 * Generic class provides template to handle interactions between cache and network.
 */
public abstract class NetworkBoundResource<CacheObject, RequestObject> {

    private static final String TAG = "NetworkBoundResource";

    private AppExecutors appExecutors;
    private MediatorLiveData<Resource<CacheObject>> results = new MediatorLiveData<>();

    // executor manages background threads
    public NetworkBoundResource(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
    }

    // initialize
    private void init() {

        // update loading status
        results.setValue((Resource<CacheObject>) Resource.loading(null));

        // get separate livedata for cache result
        final LiveData<CacheObject> dbSource = loadFromDb();

        // add cache source to the mediator
        results.addSource(dbSource, new Observer<CacheObject>() {

            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {

                // result has been retrieved from cache so remove this source
                results.removeSource(dbSource);

                // cache data is stale
                if (shouldFetch(cacheObject)){
                    // todo get data from the network
                }

                // cache data is valid
                else {

                    // add cache source back to mediator to update observers
                    results.addSource(dbSource, new Observer<CacheObject>() {

                        // triggers immediately to initialize
                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {

                            // update observers
                            setValue(Resource.success(cacheObject));
                        }
                    });
                }
            }
        });
    }

    // ensure cache data is new before emitting
    private void setValue(Resource<CacheObject> newValue){

        // catch when user goes back and forth on same element
        if(results.getValue() != newValue){
            results.setValue(newValue);
        }
    }

    // save result of API response into database
    @WorkerThread
    protected abstract void saveCallResult(@NonNull RequestObject item);

    // decision whether to fetch updated data from network
    @MainThread
    protected abstract boolean shouldFetch(@Nullable CacheObject data);

    // get cached data
    @NonNull @MainThread
    protected abstract LiveData<CacheObject> loadFromDb();

    // create API call
    @NonNull @MainThread
    protected abstract LiveData<ApiResponse<RequestObject>> createCall();

    // Return livedata object that represents the resource implemented in base class
    public final LiveData<Resource<CacheObject>> getAsLiveData(){
        return results;
    }
}
