package com.codingwithmitch.foodrecipes.util;

import android.arch.lifecycle.LiveData;

import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;

import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Custom call adapter for Retrofit converts Call objects to LiveData.
 */
public class LiveDataCallAdapter<R> implements CallAdapter<R, LiveData<ApiResponse<R>>> {

    // response type is either RecipeResponse or RecipeSearchResponse
    private Type responseType;
    public LiveDataCallAdapter(Type responseType) {
        this.responseType = responseType;
    }

    @Override
    public Type responseType() {
        return responseType;
    }

    @Override
    public LiveData<ApiResponse<R>> adapt(final Call<R> call) {

        return new LiveData<ApiResponse<R>>(){

            @Override
            protected void onActive() {
                super.onActive();

                // create generic api response
                final ApiResponse apiResponse = new ApiResponse();

                // critical part here: when the call is made wrap the response into generic
                call.enqueue(new Callback<R>() {

                    @Override
                    public void onResponse(Call<R> call, Response<R> response) {

                        // wrap the response and post it
                        postValue(apiResponse.create(response));
                    }

                    @Override
                    public void onFailure(Call<R> call, Throwable t) {

                        // wrap the response and post it
                        postValue(apiResponse.create(t));
                    }
                });
            }
        };
    }

}
