package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.model.Query;
import com.sam_chordas.android.stockhawk.model.QueryResponse;
import com.sam_chordas.android.stockhawk.model.Quote;
import com.sam_chordas.android.stockhawk.model.Results;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockService;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.sam_chordas.android.stockhawk.rest.Utils.saveStockQuotes;

/**
 * Created by Bhupendra Singh on 22/4/16.
 */
public  class DetailActivity extends AppCompatActivity {

    private LineChart chart;
    private LineData data;
    private String LOG_TAG =DetailActivity.class.getSimpleName();
    OkHttpClient okClient  = new OkHttpClient();
    private boolean isConnected =false;
    private Context mContext;
    private Intent intent;
    private StockService.RetrieveHistoryService retrieveHistoryService;
    private static final String API_BASE_URL ="https://query.yahooapis.com/v1/public/";

    private Call<QueryResponse> callQueryResponse;
    private QueryResponse queryResponse;
    private int quoteCount=0;
    private ProgressBar mProgressBar;
    private String stock ="";

    //for fetching quotes
    public ArrayList<Quote> quoteList = new ArrayList<Quote>();

    //HashSet for storing dates and closing value for a particular stock
    public HashMap<String, Double> quoteHash;

    public static final String QUOTE_LIST_BUNDLE = "Quote_List_Bundle";

    public static final String QUOTE_HASHMAP = "Quote_Hashmap";

    //for tabbed layout
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null && savedInstanceState.containsKey(QUOTE_LIST_BUNDLE)) {
            //Orientation changed so fetch data from savedInstanceState bundle

            quoteList = savedInstanceState.getParcelableArrayList(QUOTE_LIST_BUNDLE);
            Log.d(LOG_TAG , "Inside onCreate , data loaded from bundle , quoteList size = " +quoteList.size());

            stock = intent.getStringExtra("symbol");
            quoteHash = Utils.saveStockQuotes(quoteList);
            //addQuotes(quoteHash);

            createTabbedLayout();
        }
        else{  //query the api for data

            mContext = this;
            intent = getIntent();


            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date dateobj = new Date();

            Log.d(LOG_TAG, "Current date : "+df.format(dateobj).toString());

            String todayDate = df.format(dateobj).toString();
            String startDate = "2016-01-01";
            String resultJSON ="";

            stock = intent.getStringExtra("symbol");

            try {
                android.support.v7.app.ActionBar actionBar = getSupportActionBar();
                actionBar.setTitle(stock);
                Log.d(LOG_TAG , "Title set for actionbar");

            }
            catch (NullPointerException e){
                e.printStackTrace();
                Log.d(LOG_TAG , "Could not set title , NPE thrown");
            }



            String queryForDetail = "select * from yahoo.finance.historicaldata where symbol = \'"+stock+"\' and startDate = \'"+startDate+"\' and endDate = \'"+todayDate+"\'";
            Log.d(LOG_TAG , "Query :" +queryForDetail);


            //Check connectivity
            ConnectivityManager cm =
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();

            //for OKHttp debugging  // Delete before publishing

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // set your desired log level
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            okhttp3.OkHttpClient.Builder httpClient = new okhttp3.OkHttpClient.Builder();

            // add logging as last interceptor
            httpClient.addInterceptor(logging);  // <-- this is the important line!



            //For retrofit
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();

            retrieveHistoryService = retrofit.create(StockService.RetrieveHistoryService.class);



            if(isConnected){

                getQuotes(queryForDetail);

                for(Quote quote : quoteList){
                    Log.d(LOG_TAG , String.valueOf(quote.getClose()));
                }

            }

        }



    }

    private void createTabbedLayout(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(stock);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);


        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        Log.d(LOG_TAG , "Tabs created");
    }



    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new WeekFragment(), "1 Week");
        adapter.addFragment(new MonthFragment(), "1 Month");
        adapter.addFragment(new Month6Fragment(), "6 Months");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {

            Fragment fragment = mFragmentList.get(position);
            Bundle args = new Bundle();

            //Log.d(LOG_TAG, "Adding quotes bundle , size is = "+quoteList.size());
            // Our object is just an integer :-P
            args.putParcelableArrayList(QUOTE_HASHMAP,quoteList);
            fragment.setArguments(args);
            return fragment;

        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }



    public void getQuotes(String queryString){


        Log.d(LOG_TAG , "Inside getQuotes() , query is " +queryString );

        callQueryResponse = retrieveHistoryService.getHistory(queryString);


        callQueryResponse.enqueue(new retrofit2.Callback<QueryResponse>(){

            @Override
            public void onResponse(Call<QueryResponse> call, retrofit2.Response<QueryResponse> response) {


                if(response.body() != null){
                    Log.d(LOG_TAG , "Response body is "+ response.body().toString());
                    queryResponse = response.body();

                }

                else{ // response.body() is null and reponse.code() =400 so extract reponse from error code
                    Log.d(LOG_TAG , "Response is null , reponse code = "+response.code());
                    if (response.code() == 400 ) {
                        Log.d(LOG_TAG, "onResponse - Status : " + response.code());
                        Gson gson = new Gson();
                        TypeAdapter<QueryResponse> adapter = gson.getAdapter(QueryResponse.class);
                        try {
                            if (response.errorBody() != null)
                                queryResponse = adapter.fromJson( response.errorBody().string());
                            else{
                                Log.d(LOG_TAG , "error.body() is null");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }



                Query query = queryResponse.getQuery();
                Results results = query.getResults();
                int count = query.getCount();

                quoteList = results.getQuote();

               // mProgressBar.setVisibility(View.INVISIBLE);

                quoteHash = saveStockQuotes(quoteList);


               // addQuotes(quoteHash);

                Log.d(LOG_TAG , "getQuotes() finished  , size of quotesList =" +quoteList.size() );



               createTabbedLayout();
            }

            @Override
            public void onFailure(Call<QueryResponse> call, Throwable t) {

            }
        });

        //Log.d(LOG_TAG , "getQuotes() finished "  );
    }




    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(LOG_TAG , "Inside onSaveInstanceState , quoteList size = " +quoteList.size());
        outState.putParcelableArrayList(QUOTE_LIST_BUNDLE , quoteList);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_save_graph){
            // this is for changing stock changes from percent value to dollar value
            chart.saveToGallery("mychart.jpg", 85);
            Log.d(LOG_TAG, "Saved Graph Image");
        }

        return super.onOptionsItemSelected(item);
    }



}
































































/*
package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.model.Query;
import com.sam_chordas.android.stockhawk.model.QueryResponse;
import com.sam_chordas.android.stockhawk.model.Quote;
import com.sam_chordas.android.stockhawk.model.Results;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockService;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.R.color.white;

*/
/**
 * Created by Bhupendra Singh on 22/4/16.
*//*

public  class DetailActivity extends AppCompatActivity {

    private LineChart chart;
    private LineData data;
    private String LOG_TAG =DetailActivity.class.getSimpleName();
    OkHttpClient okClient  = new OkHttpClient();
    private boolean isConnected =false;
    private Context mContext;
    private Intent intent;
    private StockService.RetrieveHistoryService  retrieveHistoryService;
    private static final String API_BASE_URL ="https://query.yahooapis.com/v1/public/";

    private Call<QueryResponse> callQueryResponse;
    private QueryResponse queryResponse;
    private int quoteCount=0;

    //for fetching quotes
    public ArrayList<Quote> quoteList = new ArrayList<Quote>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        mContext = this;
        intent = getIntent();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date dateobj = new Date();
       // System.out.println(df.format(dateobj));

        Log.d(LOG_TAG, "Current date : "+df.format(dateobj).toString());

        String todayDate = df.format(dateobj).toString();
        String startDate = "2016-01-01";
        String stock = intent.getStringExtra("symbol");
        String resultJSON ="";

        String queryForDetail = "select * from yahoo.finance.historicaldata where symbol = \'"+stock+"\' and startDate = \'"+startDate+"\' and endDate = \'"+todayDate+"\'";

        Log.d(LOG_TAG , "Query :" +queryForDetail);


        //Check connectivity

        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        //for OKHttp debugging  // Delete before publishing

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // set your desired log level
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        okhttp3.OkHttpClient.Builder httpClient = new okhttp3.OkHttpClient.Builder();

        // add logging as last interceptor
        httpClient.addInterceptor(logging);  // <-- this is the important line!


        //For retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();

        retrieveHistoryService = retrofit.create(StockService.RetrieveHistoryService.class);


        if(isConnected){

            getQuotes(queryForDetail);

            for(Quote quote : quoteList){
                Log.d(LOG_TAG , String.valueOf(quote.getClose()));
            }

        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_save_graph){
            // this is for changing stock changes from percent value to dollar value
            chart.saveToGallery("mychart.jpg", 85);
            Log.d(LOG_TAG, "Saved Graph Image");
        }

        return super.onOptionsItemSelected(item);
    }

    public void getQuotes(String queryString){


        Log.d(LOG_TAG , "Inside getQuotes() , query is " +queryString );

        callQueryResponse = retrieveHistoryService.getHistory(queryString);


        callQueryResponse.enqueue(new retrofit2.Callback<QueryResponse>(){

            @Override
            public void onResponse(Call<QueryResponse> call, retrofit2.Response<QueryResponse> response) {


                if(response.body() != null){
                    Log.d(LOG_TAG , "Response body is "+ response.body().toString());
                    queryResponse = response.body();

                }

                else{ // response.body() is null and reponse.code() =400 so extract reponse from error code
                    Log.d(LOG_TAG , "Response is null , reponse code = "+response.code());
                    if (response.code() == 400 ) {
                        Log.d(LOG_TAG, "onResponse - Status : " + response.code());
                        Gson gson = new Gson();
                        TypeAdapter<QueryResponse> adapter = gson.getAdapter(QueryResponse.class);
                        try {
                            if (response.errorBody() != null)
                                queryResponse = adapter.fromJson( response.errorBody().string());
                            else{
                                Log.d(LOG_TAG , "error.body() is null");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }



                Query query = queryResponse.getQuery();
               Results  results = query.getResults();
                int count = query.getCount();

                quoteList = results.getQuote();

                addQuotes(quoteList);




            }

            @Override
            public void onFailure(Call<QueryResponse> call, Throwable t) {

            }
        });

    }


    // method to add quotes to graph
    public void addQuotes(ArrayList<Quote> quoteList){

        ArrayList<Entry> entries = new ArrayList<>();

        int i=0;
        for(Quote quote : quoteList){
            entries.add(new Entry( quote.getClose().floatValue(), i));
            i++;

        }

        //Create the dataSet
        LineDataSet dataset = new LineDataSet(entries, "");


        //Define the labels
        ArrayList<String> labels = new ArrayList<String>();

        int j=0;
        String friendlyDate;
        for(Quote quote : quoteList){

            friendlyDate = Utils.getDayMonth(quote.getDate().toString());

            labels.add( friendlyDate+" ");
            j++;

        }

        //Create the chart
        chart = new LineChart(getBaseContext());
        setContentView(chart);

        // Fill chart with data
        data = new LineData(labels, dataset);
        chart.setData(data);

        //Add description to the chart
        chart.setDescription("Stock close value over time");

        chart.setBackgroundColor(getResources().getColor(white));

        //Add a set of colors to chart
        dataset.setColors(ColorTemplate.COLORFUL_COLORS);

        //Animate the chart
        chart.animateY(5000);

    }

}





*/
