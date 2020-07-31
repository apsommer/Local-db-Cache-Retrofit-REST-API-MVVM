package com.codingwithmitch.foodrecipes.viewmodels;


import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.repositories.RecipeRepository;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeListViewModel extends AndroidViewModel {

    private static final String TAG = "RecipeListViewModel";

    // common organizational pattern to determine proper recycler source
    public enum ViewState {CATEGORIES, RECIPES}

    private MutableLiveData<ViewState> viewState;
    private RecipeRepository recipeRepository;
    private MediatorLiveData<Resource<List<Recipe>>> recipes = new MediatorLiveData<>();

    public RecipeListViewModel(@NonNull Application application) {
        super(application);
        recipeRepository = RecipeRepository.getInstance(application);
        init();
    }

    // recycler starts with categories
    private void init(){

        if(viewState == null){
            viewState = new MutableLiveData<>();
            viewState.setValue(ViewState.CATEGORIES);
        }
    }

    // expose view state
    public LiveData<ViewState> getViewState() {
        return viewState;
    }

    // expose livedata
    public LiveData<Resource<List<Recipe>>> getRecipes() {
        return recipes;
    }

    // call into repo
    public void searchRecipesApi(String query, int pageNumber){

        // repo source
        final LiveData<Resource<List<Recipe>>> repositorySource = recipeRepository.searchRecipesApi(query, pageNumber);

        recipes.addSource(repositorySource, new Observer<Resource<List<Recipe>>>() {

            @Override
            public void onChanged(@Nullable Resource<List<Recipe>> listResource) {

                // todo display logic
                recipes.setValue(listResource);
            }
        });
    }
}















