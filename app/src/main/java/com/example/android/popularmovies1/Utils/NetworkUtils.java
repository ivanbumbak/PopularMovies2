package com.example.android.popularmovies1.Utils;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by the Bumbs on 03/03/2018.
 */

public class NetworkUtils {

    private final static String BASE_MOVIE_URL = "https://api.themoviedb.org/3/movie/";
    private final static String QUERY_API_KEY = "api_key";
    private final static String API_KEY = ""; //Add your own API key :)

    private final static String REVIEW = "/reviews";

    //URL builder for movie details
    public static URL buildUrl(String sortMovie) {
        Uri builtUri = Uri.parse(BASE_MOVIE_URL).buildUpon()
                .appendPath(sortMovie)
                .appendQueryParameter(QUERY_API_KEY, API_KEY)
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    //URL builder for reviews
    public static URL buildReviewUrl(String id) {
        Uri builtUri = Uri.parse(BASE_MOVIE_URL + id + REVIEW).buildUpon()
                .appendQueryParameter(QUERY_API_KEY, API_KEY)
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    public static String getResponseFromHttpRequest(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream inputStream = urlConnection.getInputStream();

            Scanner scanner = new Scanner(inputStream);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if(hasInput) {
                return scanner.next();
            } else return null;
        } finally {
            urlConnection.disconnect();
        }
    }
}
