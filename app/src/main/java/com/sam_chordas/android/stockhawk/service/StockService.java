package com.sam_chordas.android.stockhawk.service;

import com.sam_chordas.android.stockhawk.model.QueryResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Bhupendra Singh on 25/4/16.
 */
public class StockService {


    public interface RetrieveHistoryService{


        @GET("yql?&format=json&diagnostics=true&env=store://datatables.org/alltableswithkeys")
        Call<QueryResponse> getHistory(
                @Query("q") String query

        );
    }

}
