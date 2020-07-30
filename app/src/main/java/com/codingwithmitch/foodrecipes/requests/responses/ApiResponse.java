package com.codingwithmitch.foodrecipes.requests.responses;

import java.io.IOException;

import retrofit2.Response;

/**
 * Generic class handles all responses from Retrofit.
 */
public class ApiResponse<T> {

    // error with throwable
    public ApiResponse<T> create(Throwable error){
        return new ApiErrorResponse<>(error.getMessage().equals("")
                ? "Unknown error\nCheck network connection" : error.getMessage());
    }

    // successful response
    public ApiResponse<T> create(Response<T> response){

        // success
        if(response.isSuccessful()){

            T body = response.body();

            // body is empty
            if(body == null || response.code() == 204) return new ApiEmptyResponse<>();

            // normal successful response
            return new ApiSuccessResponse<>(body);
        }

        // error
        else{

            String errorMsg;

            // extract the error message
            try {
                if (response.errorBody() != null) errorMsg = response.errorBody().string();
                else errorMsg = "Unknown error";

            } catch (IOException e) {
                errorMsg = response.message();
                e.printStackTrace();
            }

            return new ApiErrorResponse<>(errorMsg);
        }
    }

    /**
     * Generic success response contains data in its body.
     */
    public class ApiSuccessResponse<T> extends ApiResponse<T> {

        private T body;

        ApiSuccessResponse(T body) {
            this.body = body;
        }

        public T getBody() {
            return body;
        }
    }

    /**
     * Generic error response contains a message.
     */
    public class ApiErrorResponse<T> extends ApiResponse<T> {

        private String errorMessage;

        ApiErrorResponse(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Generic empty response handles HTTP 204, ensures ApiSuccessResponse body is not null.
     */
    public class ApiEmptyResponse<T> extends ApiResponse<T> {}
}
