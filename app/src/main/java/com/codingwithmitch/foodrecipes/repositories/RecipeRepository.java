package com.codingwithmitch.foodrecipes.repositories;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.persistence.RecipeDao;
import com.codingwithmitch.foodrecipes.persistence.RecipeDatabase;
import com.codingwithmitch.foodrecipes.requests.ServiceGenerator;
import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeSearchResponse;
import com.codingwithmitch.foodrecipes.util.NetworkBoundResource;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeRepository {

    private static final String TAG = "RecipeRepository";

    private static RecipeRepository instance;
    private RecipeDao recipeDao;

    // singleton
    public static RecipeRepository getInstance(Context context){
        if(instance == null){
            instance = new RecipeRepository(context);
        }
        return instance;
    }

    // database class needs context to instantiate
    private RecipeRepository(Context context) {
        recipeDao = RecipeDatabase.getInstance(context).getRecipeDao();
    }

    // exposed to viewmodel, this is the magical part that leverages the generic networkboundresource class
    public LiveData<Resource<List<Recipe>>> searchRecipesApi(final String query, final int pageNumber){

        return new NetworkBoundResource<List<Recipe>, RecipeSearchResponse>(AppExecutors.getInstance() ){

            @Override
            public void saveCallResult(@NonNull RecipeSearchResponse item) {

                // null if api key is expired, etc
                if (item.getRecipes() == null) return;

                // create array from list
                Recipe[] recipes = new Recipe[item.getRecipes().size()];

                int index = 0;
                for(long rowId: recipeDao.insertRecipes((Recipe[]) (item.getRecipes().toArray(recipes)))){

                    // conflict defers to special insert method to avoid overwriting ingredients and timestamp
                    if(rowId == -1) {

                        Log.d(TAG, "saveCallResult: CONFLICT... This recipe is already in cache.");

                        recipeDao.updateRecipe(
                                recipes[index].getRecipe_id(),
                                recipes[index].getTitle(),
                                recipes[index].getPublisher(),
                                recipes[index].getImage_url(),
                                recipes[index].getSocial_rank()
                        );
                    }

                    index++;
                }

            }

            @Override
            public boolean shouldFetch(@Nullable List<Recipe> data) {
                return true; // always query the network since the queries can be anything
            }

            @NonNull
            @Override
            public LiveData<List<Recipe>> loadFromDb() {
                return recipeDao.searchRecipes(query, pageNumber);
            }

            @NonNull
            @Override
            public LiveData<ApiResponse<RecipeSearchResponse>> createCall() {
                return ServiceGenerator.getRecipeApi().searchRecipe(query, String.valueOf(pageNumber));
            }

        }.getAsLiveData();
    }
}
