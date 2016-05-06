package com.sam_chordas.android.stockhawk.model;

/**
 * Created by Bhupendra Singh on 25/4/16.
 */

public class Quote {

    private String Symbol;
    private Double High;
    private Double Low;
    private Double Close;
    private Long Volume;
    private String Date;


    public String getDate() {
        return Date;
    }

    public Double getClose() {
        return Close;
    }

    public String getSymbol() {
        return Symbol;
    }


}
