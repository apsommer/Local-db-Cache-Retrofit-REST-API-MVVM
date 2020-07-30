package com.codingwithmitch.foodrecipes.util;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;

/**
 * Generic class provides template to handle interactions between cache and network.
 */
public abstract class NetworkBoundResource<CacheObject, RequestObject> {

    // expose to UI
    private MediatorLiveData<Resource<CacheObject>> results = new MediatorLiveData<>();

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
