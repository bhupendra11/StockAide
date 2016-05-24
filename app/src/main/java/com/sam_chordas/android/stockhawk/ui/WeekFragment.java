package com.sam_chordas.android.stockhawk.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
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
public class WeekFragment extends Fragment {

    public ArrayList<Quote> quoteList = new ArrayList<Quote>();
    //HashSet for storing dates and closing value for a particular stock
    public HashMap<String, Double> quoteHash;
    public static final int DAYS_IN_A_WEEK = 7;

    public WeekFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_week, container, false);
        quoteList = getArguments().getParcelableArrayList(DetailActivity.QUOTE_HASHMAP);

        quoteHash = Utils.saveStockQuotes(quoteList);

        int chartViewId = R.id.week_chart;

        Utils.addQuotes(quoteHash ,quoteList , rootView ,DAYS_IN_A_WEEK ,chartViewId);

        rootView.setContentDescription(getString(R.string.a11y_graph_desc,String.valueOf(DAYS_IN_A_WEEK)));

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }



}
