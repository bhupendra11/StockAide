package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.graphics.Color;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.model.Quote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  public static boolean showPercent = true;

  public static ArrayList quoteJsonToContentVals(String JSON){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");

          /**
          Check here if the stock exists or not
          Only start batchOperation if stock exists else return empty List
          This can be done by checking the value of Bid , if it is null , the stock does not exist;
          */
          String BidPrice = jsonObject.getString("Bid");

          if (BidPrice.equals("null")){
            //Empty arrayList is returned here
          }
          else{
            //Here since the stock exists so start the batch operations
            batchOperations.add(buildBatchOperation(jsonObject));
          }
        }
        else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperation(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e){
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){

    //handle error in case no stock found
    if(bidPrice ==null){
      return "";
    }
    else{
      bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
      return bidPrice;
    }

  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);
    try {
      String change = jsonObject.getString("Change");
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));

      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));


      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString("ChangeinPercent"), true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }

    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }



  public static String getDayMonth(String date){
    int monthNo = Integer.parseInt(date.substring(5,7));
    String year = date.substring(0,4);
    String day = date.substring(8);
    String month="";
    switch (monthNo){
      case  1:
        month ="Jan";
        break;
      case 2:
        month = "Feb";
        break;
      case 3:
        month ="March";
        break;
      case 4:
        month ="April";
        break;
      case 5:
        month = "May";
        break;
      case 6:
        month ="June";
        break;
      case  7:
        month ="July";
        break;
      case 8:
        month = "August";
        break;
      case 9:
        month ="Sept";
        break;
      case  10:
        month ="Oct";
        break;
      case 11:
        month = "Nov";
        break;
      case 12:
        month ="Dec";
        break;
      default:

    }

    String result = day+" "+month;
    return  result;
  }


  public static HashMap<String, Double> saveStockQuotes(ArrayList<Quote> quoteList){
    HashMap<String, Double> qHash= new LinkedHashMap<String, Double>();

    for(Quote quote : quoteList){
      qHash.put(quote.getDate() , quote.getClose());

    }
    return qHash;
  }


  // method to add quotes to graph
  public static void addQuotes(HashMap<String, Double> quoteHash ,ArrayList<Quote> quoteList , View rootView, int days , int chartViewId)  {

    LineChart chart;
    LineData data;

    ArrayList<Entry> entries = new ArrayList<>();

    //Define the labels
    ArrayList<String> labels = new ArrayList<>();

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    Date curDate = new Date();

    Date dateobj = new Date();
    try {
      curDate =df.parse(dateobj.toString());
    }
    catch (ParseException pe){
      pe.printStackTrace();
    }


    int i=0;
    String friendlyDate;
    for (Map.Entry<String, Double> quote: quoteHash.entrySet()){

      Date date = new Date();
      try {
        date = df.parse(quote.getKey());
      }
      catch (ParseException pe){
        pe.printStackTrace();
      }

      long duration = curDate.getTime() - date.getTime();
      long diffInDays = TimeUnit.DAYS.convert(duration, TimeUnit.MILLISECONDS);


      if(diffInDays <= days) {
        entries.add(new Entry(Float.parseFloat(quote.getValue().toString()), i));
        friendlyDate = Utils.getDayMonth(quote.getKey().toString());
        labels.add(friendlyDate + " ");
        i++;
      }
      else
        break;
    }
    //Create the dataSet
    LineDataSet dataset = new LineDataSet(entries, "");



    //Create the chart
    chart = (LineChart) rootView.findViewById(chartViewId);


    // Fill chart with data
    data = new LineData(labels, dataset);
    chart.setData(data);

    //Add description to the chart
    chart.setDescription("Stock close value over time");

    chart.setBackgroundColor(Color.parseColor("#ffffff"));

    //Add a set of colors to chart
    dataset.setColors(ColorTemplate.COLORFUL_COLORS);

    //Animate the chart
    chart.animateX(1000);
    chart.animateY(1000);




  }

}
