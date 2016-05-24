package com.sam_chordas.android.stockhawk.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.model.Quote;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 */
public class Month6Fragment extends Fragment {


    public static final String LOG_TAG = Month6Fragment.class.getSimpleName();
    public ArrayList<Quote> quoteList = new ArrayList<Quote>();
    //HashSet for storing dates and closing value for a particular stock
    public HashMap<String, Double> quoteHash;

    public static final int DAYS_IN_6_MONTHS = 180;

    public Month6Fragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_month6, container, false);
        quoteList = getArguments().getParcelableArrayList(DetailActivity.QUOTE_HASHMAP);
        Log.d(LOG_TAG , "Inside onCreateView , data loaded from bundle , quoteList size = " +quoteList.size());

        quoteHash = Utils.saveStockQuotes(quoteList);

        int chartViewId = R.id.month_6_chart;

        Utils.addQuotes(quoteHash ,quoteList , rootView ,DAYS_IN_6_MONTHS,chartViewId );

        rootView.setContentDescription(getString(R.string.a11y_graph_desc,DAYS_IN_6_MONTHS));


        return rootView;
    }

}
