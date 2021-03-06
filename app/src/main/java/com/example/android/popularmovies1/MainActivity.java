package com.example.android.popularmovies1;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.popularmovies1.Adapter.MovieAdapter;
import com.example.android.popularmovies1.Data.JsonData;
import com.example.android.popularmovies1.Data.MovieData;
import com.example.android.popularmovies1.Database.FavoriteContract;
import com.example.android.popularmovies1.Utils.NetworkUtils;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private final static String popular = "popular";
    private final static String topRated = "top_rated";
    private final static String favorite = "favorite";

    private final static String MOVIE_LIST_KEY = "movieList";

    private final static String PREF_NAME = "pref";
    private final static String PREF_SORT_KEY = "sort";
    private final static String SCROLL_STATE_KEY = "scrollState";

    @BindView(R.id.recycle_view)
    RecyclerView recyclerView;

    private List<MovieData> movieDataList;
    private MovieAdapter movieAdapter;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private MovieAdapter.OnItemClickListener mListener;
    private GridLayoutManager mGridLayoutManager;
    Parcelable savedScrollState;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        movieDataList = new ArrayList<>();

        mListener = new MovieAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MovieData movieData) {
                Intent intent = new Intent(MainActivity.this, MovieDetails.class);
                intent.putExtra(getString(R.string.movie_id), movieData.getMovieId());
                intent.putExtra(getString(R.string.movie_poster), movieData.getPoster());
                intent.putExtra(getString(R.string.movie_title), movieData.getTitle());
                intent.putExtra(getString(R.string.movie_release_date), movieData.getReleaseDate());
                intent.putExtra(getString(R.string.movie_average_vote), movieData.getRating());
                intent.putExtra(getString(R.string.movie_synopsis), movieData.getSynopsis());
                getApplicationContext().startActivity(intent);
            }
        };

        preferences = this.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
        editor.apply();

        movieAdapter = new MovieAdapter(this, movieDataList, mListener);

        if(savedInstanceState != null) {
            movieDataList = savedInstanceState.getParcelableArrayList(MOVIE_LIST_KEY);
            recyclerView.setAdapter(movieAdapter);
            savedScrollState = savedInstanceState.getParcelable(SCROLL_STATE_KEY);
            if(recyclerView.getLayoutManager() != null) {
                recyclerView.getLayoutManager().onRestoreInstanceState(savedScrollState);
            }
        } else {
            recyclerView.setAdapter(movieAdapter);
            if(recyclerView.getLayoutManager() != null) {
                recyclerView.getLayoutManager().onRestoreInstanceState(savedScrollState);
            }
            if (preferences.getString(PREF_SORT_KEY, popular).equals(favorite)) {
                displayFavMovies();
            } else {
                getMovies();
            }
        }

        //Making different UI layout for portrait and for landscape orientation
        if(this.getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mGridLayoutManager = new GridLayoutManager(this, 3);
        } else if(this.getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_PORTRAIT) {
            mGridLayoutManager = new GridLayoutManager(this, 2);
        }

        recyclerView.setLayoutManager(mGridLayoutManager);
        recyclerView.setAdapter(new MovieAdapter(this, movieDataList,
                new MovieAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MovieData movieData) {
                Intent intent = new Intent(MainActivity.this, MovieDetails.class);
                intent.putExtra(getString(R.string.movie_id), movieData.getMovieId());
                intent.putExtra(getString(R.string.movie_poster), movieData.getPoster());
                intent.putExtra(getString(R.string.movie_title), movieData.getTitle());
                intent.putExtra(getString(R.string.movie_release_date), movieData.getReleaseDate());
                intent.putExtra(getString(R.string.movie_average_vote), movieData.getRating());
                intent.putExtra(getString(R.string.movie_synopsis), movieData.getSynopsis());
                getApplicationContext().startActivity(intent);
            }
        }));
    }

    //onSaveInstanceState method
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(MOVIE_LIST_KEY, (ArrayList<MovieData>) movieDataList);
        outState.putParcelable(SCROLL_STATE_KEY, recyclerView.getLayoutManager().onSaveInstanceState());
    }

    //onResume method
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onResume() {
        super.onResume();
        if(preferences.getString(PREF_SORT_KEY,
                popular).equals(favorite)) {
            displayFavMovies();
        } else {
            getMovies();
        }
    }

    //Method for getting movies via MovieAsyncTask class
    public void getMovies() {
        String gettingChoice = preferences.getString(PREF_SORT_KEY, popular);
        MovieAsyncTask movieAsyncTask = new MovieAsyncTask();
        movieAsyncTask.execute(gettingChoice);
        recyclerView.setAdapter(movieAdapter);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private Cursor getAll() {
       return getContentResolver().query(FavoriteContract.FavoriteEntry.CONTENT_URI,
               null,
               null,
               null,
               null);
    }

    //Method for retrieving list of movies from database
    private List<MovieData> retrieveFromDb(Cursor c) {
        List<MovieData> favMovieList = new ArrayList<>();
        for(int i = 0; i < c.getCount(); i++) {
            if(!c.moveToPosition(i)) {
                return null;
            }

            int id = c.getInt(c.getColumnIndex(FavoriteContract
                    .FavoriteEntry.COLUMN_ID));
            String poster = c.getString(c.getColumnIndex(FavoriteContract
                    .FavoriteEntry.COLUMN_POSTER));
            String title = c.getString(c.getColumnIndex(FavoriteContract
                    .FavoriteEntry.COLUMN_TITLE));
            String releaseDate = c.getString(c.getColumnIndex(FavoriteContract
                    .FavoriteEntry.COLUMN_RELEASE_DATE));
            double rating = c.getDouble(c.getColumnIndex(FavoriteContract
                    .FavoriteEntry.COLUMN_AVERAGE_VOTE));
            String synopsis = c.getString(c.getColumnIndex(FavoriteContract
                    .FavoriteEntry.COLUMN_SYNOPSIS));

            MovieData movieData = new MovieData(id, poster, title, releaseDate, rating, synopsis);
            favMovieList.add(movieData);
        }

        return  favMovieList;
    }

    //Method for displaying favorite movies
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void displayFavMovies() {
        movieDataList.clear();
        movieDataList = retrieveFromDb(getAll());
        movieAdapter = new MovieAdapter(this, movieDataList, mListener);
        recyclerView.setAdapter(movieAdapter);
    }

    //Method for creating menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //Method for selecting menu item
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int selectedItem = item.getItemId();
        switch (selectedItem) {
            case R.id.popularity_sort:
                editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                editor.putString(PREF_SORT_KEY, popular);
                editor.apply();
                MovieAsyncTask asyncTaskPopular = new MovieAsyncTask();
                asyncTaskPopular.execute(popular);
                break;
            case R.id.top_rated_sort:
                editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                editor.putString(PREF_SORT_KEY, topRated);
                editor.apply();
                MovieAsyncTask asyncTaskTopRated = new MovieAsyncTask();
                asyncTaskTopRated.execute(topRated);
                break;
            case R.id.favorite_sort:
                editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                editor.putString(PREF_SORT_KEY, favorite);
                editor.apply();
                displayFavMovies();
        }
        return super.onOptionsItemSelected(item);
    }

    public class MovieAsyncTask extends AsyncTask<String, Void, List<MovieData>> {

        Context context;
        MovieAdapter movieAdapter;

        MovieAdapter.OnItemClickListener listener = new MovieAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MovieData movieData) {
                Intent intent = new Intent(MainActivity.this, MovieDetails.class);
                intent.putExtra(getString(R.string.movie_id), movieData.getMovieId());
                intent.putExtra(getString(R.string.movie_poster), movieData.getPoster());
                intent.putExtra(getString(R.string.movie_title), movieData.getTitle());
                intent.putExtra(getString(R.string.movie_release_date), movieData.getReleaseDate());
                intent.putExtra(getString(R.string.movie_average_vote), movieData.getRating());
                intent.putExtra(getString(R.string.movie_synopsis), movieData.getSynopsis());
                getApplicationContext().startActivity(intent);
            }
        };

        @Override
        protected List<MovieData> doInBackground(String... strings) {
            String rawData = "";
            List<MovieData> movieList = new ArrayList<>();
            try {
                rawData = NetworkUtils.getResponseFromHttpRequest(NetworkUtils.buildUrl(strings[0]));
                movieList = JsonData.getDataFromJson(rawData);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("MovieAsyncTask", "Error in MovieAsyncTask");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("JSONAsync", "JSON problem");
            }

            return movieList;
        }

        @Override
        protected void onPostExecute(List<MovieData> movieData) {
            movieDataList.clear();
            movieDataList.addAll(movieData);
            movieAdapter = new MovieAdapter(context, movieDataList, listener);
            recyclerView.getLayoutManager().onRestoreInstanceState(savedScrollState);
            recyclerView.setAdapter(movieAdapter);
        }
    }
}
