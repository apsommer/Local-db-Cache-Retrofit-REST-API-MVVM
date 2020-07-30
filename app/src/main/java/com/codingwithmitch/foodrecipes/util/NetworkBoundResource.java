package com.codingwithmitch.foodrecipes.util;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;

/**
 * Single source of truth is the cache. When a resource is requested data in the cache is
 * immediately returned. A decision is then made about whether to update this data via a network
 * call. If a network call is desired, the call is made and the result is put in the cache.
 */
public abstract class NetworkBoundResource<CacheObject, RequestObject> {

    private static final String TAG = "NetworkBoundResource";

    private AppExecutors appExecutors;
    private MediatorLiveData<Resource<CacheObject>> results = new MediatorLiveData<>();

    // executor manages background threads
    public NetworkBoundResource(AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
        init();
    }

    // initialize
    private void init() {

        // emit that cache object (currently null) is loading
        results.setValue((Resource<CacheObject>) Resource.loading(null));

        // separate livedata for cache result
        final LiveData<CacheObject> dbSource = loadFromDb();

        // add cache source to the mediator livedata, triggers onChanged immediately
        results.addSource(dbSource, new Observer<CacheObject>() {

            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {

                // result is retrieved from cache so remove it from mediator
                results.removeSource(dbSource);

                // cache data is stale, get new data from network
                if (shouldFetch(cacheObject)) fetchFromNetwork(dbSource);

                // cache data is not stale
                else {

                    // add cache source back into mediator to update observers
                    results.addSource(dbSource, new Observer<CacheObject>() {

                        // triggers immediately to initialize
                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {

                            // update status to success and emit to observers
                            setValue(Resource.success(cacheObject));
                        }
                    });
                }
            }
        });
    }

    private void fetchFromNetwork(final LiveData<CacheObject> dbSource) {

        Log.d(TAG, "fetchFromNetwork: called.");

        // update status to loading
        results.addSource(dbSource, new Observer<CacheObject>() {

            @Override
            public void onChanged(@Nullable CacheObject cacheObject) {
                setValue(Resource.loading(cacheObject));
            }
        });

        // separate livedata for emitting results of network call
        final LiveData<ApiResponse<RequestObject>> apiResponse = createCall();

        // add network data source
        results.addSource(apiResponse, new Observer<ApiResponse<RequestObject>>() {

            @Override
            public void onChanged(@Nullable final ApiResponse<RequestObject> requestObjectApiResponse) {

                // remove both source from mediator
                results.removeSource(dbSource);
                results.removeSource(apiResponse);

                // success
                if(requestObjectApiResponse instanceof ApiResponse.ApiSuccessResponse){

                    Log.d(TAG, "onChanged: ApiSuccessResponse.");

                    // todo swapping between background and main thread is not necessary here, just use postValue and setValue directly

                    // execute runnable on background thread
                    appExecutors.diskIO().execute(new Runnable() {

                        @Override
                        public void run() {

                            // save network response to cache
                            saveCallResult((RequestObject) processResponse((ApiResponse.ApiSuccessResponse) requestObjectApiResponse));

                            // execute runnable on main thread
                            appExecutors.mainThread().execute(new Runnable() {

                                @Override
                                public void run() {

                                    // resume observing cache
                                    results.addSource(loadFromDb(), new Observer<CacheObject>() {

                                        @Override
                                        public void onChanged(@Nullable CacheObject cacheObject) {
                                            setValue(Resource.success(cacheObject));
                                        }
                                    });
                                }
                            });
                        }
                    });
                }

                // empty network response
                else if(requestObjectApiResponse instanceof ApiResponse.ApiEmptyResponse){

                    Log.d(TAG, "onChanged: ApiEmptyResponse");

                    // execute runnable on main thread
                    appExecutors.mainThread().execute(new Runnable() {

                        @Override
                        public void run() {

                            // resume observing cache
                            results.addSource(loadFromDb(), new Observer<CacheObject>() {

                                @Override
                                public void onChanged(@Nullable CacheObject cacheObject) {
                                    setValue(Resource.success(cacheObject));
                                }
                            });
                        }
                    });
                }

                // network response error
                else if(requestObjectApiResponse instanceof ApiResponse.ApiErrorResponse){

                    Log.d(TAG, "onChanged: ApiErrorResponse.");

                    // execute runnable on main thread
                    results.addSource(dbSource, new Observer<CacheObject>() {

                        @Override
                        public void onChanged(@Nullable CacheObject cacheObject) {
                            setValue(Resource.error((
                                    (ApiResponse.ApiErrorResponse) requestObjectApiResponse).getErrorMessage(), cacheObject));
                        }
                    });
                }
            }
        });

    }

    // convert successful network response into cache object
    private CacheObject processResponse(ApiResponse.ApiSuccessResponse response){
        return (CacheObject) response.getBody();
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
