package com.bhupendra.android.stockhawk.ui;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bhupendra.android.stockhawk.R;
import com.bhupendra.android.stockhawk.model.Query;
import com.bhupendra.android.stockhawk.model.QueryResponse;
import com.bhupendra.android.stockhawk.model.Quote;
import com.bhupendra.android.stockhawk.model.Results;
import com.bhupendra.android.stockhawk.rest.Utils;
import com.bhupendra.android.stockhawk.service.StockService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.bhupendra.android.stockhawk.rest.Utils.saveStockQuotes;

/**
 * Created by Bhupendra Singh on 22/4/16.
 */
public  class DetailActivity extends AppCompatActivity {

    private LineChart chart;
    private LineData data;
    OkHttpClient okClient  = new OkHttpClient();
    private boolean isConnected =false;
    private Context mContext;
    private Intent intent;
    private StockService.RetrieveHistoryService retrieveHistoryService;
    private static final String API_BASE_URL ="https://query.yahooapis.com/v1/public/";

    private Call<QueryResponse> callQueryResponse;
    private QueryResponse queryResponse;
    private String stock ="";

    //for fetching quotes
    public ArrayList<Quote> quoteList = new ArrayList<Quote>();

    //HashSet for storing dates and closing value for a particular stock
    public HashMap<String, Double> quoteHash;

    public static final String QUOTE_LIST_BUNDLE = "Quote_List_Bundle";

    public static final String QUOTE_HASHMAP = "Quote_Hashmap";

    // Here we display data for a max for 6 moths , so fetching data for 7 months approx
    public static final int DAYS_TO_FETCH_DATA_FOR = 30*7;

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


        intent = getIntent();
        stock = intent.getStringExtra("symbol");

        try {
            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(stock);

        }
        catch (NullPointerException e){
            e.printStackTrace();
        }


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        if (savedInstanceState != null && savedInstanceState.containsKey(QUOTE_LIST_BUNDLE)) {
            //Orientation changed so fetch data from savedInstanceState bundle


            quoteList = savedInstanceState.getParcelableArrayList(QUOTE_LIST_BUNDLE);

            quoteHash = Utils.saveStockQuotes(quoteList);
            //addQuotes(quoteHash);



            createTabbedLayout();
        }
        else{  //query the api for data

            mContext = this;

            //mProgressBar.setVisibility(View.VISIBLE);

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date todayDateObj = new Date();

            long daysToSub =  210*1000 * 60 * 60 * 24;



            Calendar c = Calendar.getInstance();
            c.setTime(todayDateObj);
            c.add(Calendar.DATE, -DAYS_TO_FETCH_DATA_FOR);
            Date start = c.getTime();


            String todayDate = df.format(todayDateObj).toString();
            String startDate = df.format(start).toString();

            String resultJSON ="";



            String queryForDetail = "select * from yahoo.finance.historicaldata where symbol = \'"+stock+"\' and startDate = \'"+startDate+"\' and endDate = \'"+todayDate+"\'";

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

            }
            else{
                LinearLayout loadingHistoryView = (LinearLayout) findViewById(R.id.loadingLayout);
                loadingHistoryView.setVisibility(View.GONE);
                TextView emptyDetailView = (TextView) findViewById(R.id.detail_empty_textview);
                emptyDetailView.setVisibility(View.VISIBLE);
            }


        }


    }

    private void createTabbedLayout(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);


        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

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

        callQueryResponse = retrieveHistoryService.getHistory(queryString);
        callQueryResponse.enqueue(new retrofit2.Callback<QueryResponse>(){

            @Override
            public void onResponse(Call<QueryResponse> call, retrofit2.Response<QueryResponse> response) {

                if(response.body() != null){
                    queryResponse = response.body();

                }

                else{ // response.body() is null and reponse.code() =400 so extract reponse from error code
                    if (response.code() == 400 ) {
                        Gson gson = new Gson();
                        TypeAdapter<QueryResponse> adapter = gson.getAdapter(QueryResponse.class);
                        try {
                            if (response.errorBody() != null)
                                queryResponse = adapter.fromJson( response.errorBody().string());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }



                Query query = queryResponse.getQuery();
                Results results = query.getResults();

                quoteList = results.getQuote();

                quoteHash = saveStockQuotes(quoteList);

               createTabbedLayout();
            }

            @Override
            public void onFailure(Call<QueryResponse> call, Throwable t) {

            }
        });

    }




    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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


        return super.onOptionsItemSelected(item);
    }



}


























































