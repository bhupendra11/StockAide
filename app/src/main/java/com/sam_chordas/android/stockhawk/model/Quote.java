package com.sam_chordas.android.stockhawk.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Bhupendra Singh on 25/4/16.
 */

public class Quote implements Parcelable {

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



    protected Quote(Parcel in) {
        Symbol = in.readString();
        High = in.readByte() == 0x00 ? null : in.readDouble();
        Low = in.readByte() == 0x00 ? null : in.readDouble();
        Close = in.readByte() == 0x00 ? null : in.readDouble();
        Volume = in.readByte() == 0x00 ? null : in.readLong();
        Date = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Symbol);
        if (High == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(High);
        }
        if (Low == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(Low);
        }
        if (Close == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeDouble(Close);
        }
        if (Volume == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeLong(Volume);
        }
        dest.writeString(Date);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Quote> CREATOR = new Parcelable.Creator<Quote>() {
        @Override
        public Quote createFromParcel(Parcel in) {
            return new Quote(in);
        }

        @Override
        public Quote[] newArray(int size) {
            return new Quote[size];
        }
    };


}
