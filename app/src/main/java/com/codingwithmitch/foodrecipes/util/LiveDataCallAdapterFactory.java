package com.codingwithmitch.foodrecipes.util;

import android.arch.lifecycle.LiveData;

import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Factory class is needed for Retrofit instance builder. All these checks are overkill, we will
 * only ever pass correct objects. :/
 */
public class LiveDataCallAdapterFactory extends CallAdapter.Factory {

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {

        // ensure adapter is returning livedata
        if(CallAdapter.Factory.getRawType(returnType) != LiveData.class) return null;

        // ensure live data is wrapping an ApiResponse
        Type observableType = CallAdapter.Factory.getParameterUpperBound(0, (ParameterizedType) returnType);
        Type rawObservableType = CallAdapter.Factory.getRawType(observableType);
        if(rawObservableType != ApiResponse.class) throw new IllegalArgumentException("Type must be defined resource.");

        // ensure ApiResponse is parameterized
        if(!(observableType instanceof ParameterizedType)) throw new IllegalArgumentException("resource must be parameterized");

        // get the Response type
        Type bodyType = CallAdapter.Factory.getParameterUpperBound(0, (ParameterizedType) observableType);
        return new LiveDataCallAdapter<Type>(bodyType);
    }
}
