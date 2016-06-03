package com.bhupendra.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bhupendra.android.stockhawk.R;
import com.bhupendra.android.stockhawk.data.QuoteColumns;
import com.bhupendra.android.stockhawk.data.QuoteProvider;

/**
 * Created by Bhupendra Singh on 12/5/16.
 */
public class WidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private Cursor mCursor ;
    private Context mContext;
    private Intent mIntent;

    public static final String[] STOCK_COLUMNS ={
            QuoteColumns._ID,
            QuoteColumns.SYMBOL,
            QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE,
            QuoteColumns.CHANGE,
            QuoteColumns.ISUP
    };

    static final int INDEX_QUOTE_ID=0;
    static final int INDEX_QUOTE_SYMBOL =1;
    static final int INDEX_QUOTE_BIDPRICE =2;
    static final int INDEX_QUOTE_PERCENT_CHANGE =3;
    static final int INDEX_QUOTE_CHANGE =4;
    static final int INDEX_QUOTE_ISUP =5;

    public WidgetFactory(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;
    }

    @Override
    public void onCreate() {
        // Nothing to do
    }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }
        // This method is called by the app hosting the widget (e.g., the launcher)
        // However, our ContentProvider is not exported so it doesn't have access to the
        // data. Therefore we need to clear (and finally restore) the calling identity so
        // that calls use our process and permission

        mCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                STOCK_COLUMNS,
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

    @Override
    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position == AdapterView.INVALID_POSITION ||
                mCursor == null || !mCursor.moveToPosition(position)) {
            return null;
        }
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(),
                R.layout.stock_widget_list_item);

        if(mCursor.moveToPosition(position)){

            String symbol = mCursor.getString(INDEX_QUOTE_SYMBOL);
            String bidPrice = mCursor.getString(INDEX_QUOTE_BIDPRICE);
            String change = mCursor.getString(INDEX_QUOTE_CHANGE);

            remoteViews.setTextViewText(R.id.stock_symbol,symbol);
            remoteViews.setTextViewText(R.id.bid_price , bidPrice);
            remoteViews.setTextViewText(R.id.change , change);

            // Content Descriptions for RemoteViews were only added in ICS MR1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                remoteViews.setContentDescription(R.id.stock_symbol , mContext.getString(R.string.a11y_stock_symbol, symbol));
                remoteViews.setContentDescription(R.id.bid_price , mContext.getString(R.string.a11y_bid_price, bidPrice));
                remoteViews.setContentDescription(R.id.change, mContext.getString(R.string.a11y_change, change));
            }


            //int isUpIndex = mCursor.getColumnIndex(QuoteColumns.ISUP);

            if (mCursor.getString(INDEX_QUOTE_ISUP).equals("1")) {
                remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
            }
            else {
                remoteViews.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
            }

            Intent newIntent = new Intent();

            newIntent.putExtra("symbol", mCursor.getString(INDEX_QUOTE_SYMBOL));
            // In setOnClickFillIntent method, the ID to be passed is of the Rootview
            // of the layout passed in the remote view - above, i.e. rootview of the list_item_quote.
            remoteViews.setOnClickFillInIntent(R.id.widget_list_item, newIntent);

        }
        return  remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (mCursor.moveToPosition(position))
            return mCursor.getLong(INDEX_QUOTE_ID);
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }



}
