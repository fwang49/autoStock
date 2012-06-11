/**
 * 
 */
package com.autoStock.display;

import java.util.ArrayList;

import com.autoStock.Co;
import com.autoStock.exchange.request.RequestHistoricalData;
import com.autoStock.exchange.request.base.RequestHolder;
import com.autoStock.exchange.request.listener.RequestHistoricalDataListener;
import com.autoStock.exchange.results.ExResultHistoricalData.ExResultRowHistoricalData;
import com.autoStock.exchange.results.ExResultHistoricalData.ExResultSetHistoricalData;
import com.autoStock.tables.TableController;
import com.autoStock.tables.TableDefinitions.AsciiTables;
import com.autoStock.tools.DateTools;
import com.autoStock.tools.MathTools;
import com.autoStock.trading.types.HistoricalData;

/**
 * @author Kevin Kowalewski
 *
 */
public class DisplayHistoricalPrices {
	
	private HistoricalData typeHistoricalData;
	
	public DisplayHistoricalPrices(HistoricalData typeHistoricalData){
		this.typeHistoricalData = typeHistoricalData;
	}
	
	public void display(){
		new RequestHistoricalData(new RequestHolder(null), new RequestHistoricalDataListener() {
			@Override
			public void failed(RequestHolder requestHolder) {
				
			}
			
			@Override
			public void completed(RequestHolder requestHolder, ExResultSetHistoricalData exResultSetHistoricalData) {
				ArrayList<ArrayList<String>> listOfRows = new ArrayList<ArrayList<String>>();
				ExResultRowHistoricalData lastRow = exResultSetHistoricalData.listOfExResultRowHistoricalData.get(0);
				
				for (ExResultRowHistoricalData exResultRowHistoricalData : exResultSetHistoricalData.listOfExResultRowHistoricalData){
					ArrayList<String> listOfColumnValues = new ArrayList<String>();
					listOfColumnValues.add(exResultSetHistoricalData.typeHistoricalData.symbol);
					listOfColumnValues.add(DateTools.getPrettyDate(exResultRowHistoricalData.date));
					listOfColumnValues.add(String.valueOf(exResultRowHistoricalData.price));
					listOfColumnValues.add(String.valueOf(exResultRowHistoricalData.volume));
					listOfColumnValues.add(String.valueOf(exResultRowHistoricalData.count));
					listOfColumnValues.add(String.valueOf(MathTools.round(exResultRowHistoricalData.price - lastRow.price)));
					listOfRows.add(listOfColumnValues);
					
					lastRow = exResultRowHistoricalData;
				}
				
				new TableController().displayTable(AsciiTables.stock_historical_price_live, listOfRows);
				Co.println("Completed!!!");
			}
		}, typeHistoricalData);
	}
}
