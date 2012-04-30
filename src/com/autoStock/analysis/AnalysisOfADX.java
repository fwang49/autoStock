/**
 * 
 */
package com.autoStock.analysis;

import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.ArrayUtils;

import com.autoStock.analysis.results.ResultsADX;
import com.autoStock.taLib.MInteger;
import com.autoStock.taLib.RetCode;
import com.autoStock.tools.DataExtractor;
import com.autoStock.types.TypeQuoteSlice;

/**
 * @author Kevin Kowalewski
 *
 */
public class AnalysisOfADX extends AnalysisBase {
	public ResultsADX results;
	
	public AnalysisOfADX(int periodLength, boolean preceedDataset) {
		super(periodLength, preceedDataset);
	}
	
	public ResultsADX analize(){
		results = new ResultsADX(endIndex+1);
		
		results.arrayOfDates =  new DataExtractor().extractDate(((ArrayList<TypeQuoteSlice>)super.dataSource), "dateTime").toArray(new Date[0]);
		results.arrayOfPrice =  new ArrayUtils().toPrimitive(new DataExtractor().extractDouble(((ArrayList<TypeQuoteSlice>)super.dataSource), "priceClose").toArray(new Double[0]));
		
		//arrayOfPriceOpen = new ArrayUtils().toPrimitive(new DataExtractor().extractDouble(((ArrayList<TypeQuoteSlice>)super.dataSource), "priceOpen").toArray(new Double[0]));
		arrayOfPriceHigh = new ArrayUtils().toPrimitive(new DataExtractor().extractDouble(((ArrayList<TypeQuoteSlice>)super.dataSource), "priceHigh").toArray(new Double[0]));
		arrayOfPriceLow = new ArrayUtils().toPrimitive(new DataExtractor().extractDouble(((ArrayList<TypeQuoteSlice>)super.dataSource), "priceLow").toArray(new Double[0]));
		arrayOfPriceClose = new ArrayUtils().toPrimitive(new DataExtractor().extractDouble(((ArrayList<TypeQuoteSlice>)super.dataSource), "priceClose").toArray(new Double[0]));
		
		if (preceedDataset){
			preceedDatasetWithPeriod();
		}
		
		RetCode returnCode = getTaLibCore().adx(0, endIndex, arrayOfPriceHigh, arrayOfPriceLow, arrayOfPriceClose, periodLength/2, new MInteger(), new MInteger(), results.arrayOfADX);
		handleAnalysisResult(returnCode);
		
		return results;
	}
}