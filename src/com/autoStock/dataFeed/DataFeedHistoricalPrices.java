/**
 * 
 */
package com.autoStock.dataFeed;

import java.util.ArrayList;

import com.autoStock.dataFeed.listener.DataFeedListenerOfQuoteSlice;
import com.autoStock.generated.basicDefinitions.BasicTableDefinitions.DbStockHistoricalPrice;
import com.autoStock.trading.platform.ib.definitions.HistoricalData.Resolution;
import com.autoStock.trading.types.TypeHistoricalData;
import com.autoStock.types.TypeQuoteSlice;

/**
 * @author Kevin Kowalewski
 *
 */
public class DataFeedHistoricalPrices {
	private int feedInterval;
	private TypeHistoricalData typeHistoricalData;
	private ArrayList<DataFeedListenerOfQuoteSlice> listOfListener = new ArrayList<DataFeedListenerOfQuoteSlice>();
	private ArrayList<DbStockHistoricalPrice> listOfPrices;
	private Resolution resolution;
	private Thread threadForDelivery;
	
	public DataFeedHistoricalPrices(TypeHistoricalData typeHistoricalData, ArrayList<DbStockHistoricalPrice> listOfPrices){
		this.typeHistoricalData = typeHistoricalData;
		this.listOfPrices = listOfPrices;
		this.resolution = typeHistoricalData.resolution;
	}
	
	public void startFeed(int feedIntervalSec, final int delayMsec){
		if (feedIntervalSec < 1){throw new IllegalArgumentException();}
		
		threadForDelivery = new Thread(new Runnable(){
			@Override
			public void run() {
				for (DbStockHistoricalPrice price : listOfPrices){
					feed(price);
					try {Thread.sleep(delayMsec);}catch(InterruptedException e){return;}
				}
				
				feedFinished();
			}
		});
		
		threadForDelivery.setPriority(Thread.MIN_PRIORITY);
		threadForDelivery.start();
	}
	
	private void feedFinished(){
		for (DataFeedListenerOfQuoteSlice listener : listOfListener){
			listener.endOfFeed();
		}
	}
	
	private void feed(DbStockHistoricalPrice price){
		for (DataFeedListenerOfQuoteSlice listener : listOfListener){
			listener.receivedQuoteSlice(new TypeQuoteSlice(price.symbol, price.priceOpen, price.priceHigh, price.priceLow, price.priceClose, -1, -1, price.sizeVolume, price.dateTime, resolution));
		}
	}
	
	public void addListener(DataFeedListenerOfQuoteSlice listener){
		listOfListener.add(listener);
	}
	
	public void removeListener(DataFeedListenerOfQuoteSlice listener){
		listOfListener.remove(listener);
	}
}