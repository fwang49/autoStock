/**
 * 
 */
package com.autoStock.signal;

import com.autoStock.signal.SignalDefinitions.SignalMetricType;

/**
 * @author Kevin Kowalewski
 *
 */
public class SignalOfSTORSI extends SignalBase {
	private double percentKValue = 0;
	private double percentDValue = 0;
	
	public SignalOfSTORSI(double[] arrayOfPercentK, double[] arrayOfPercentD, int periodAverage){
		super(SignalMetricType.metric_storsi);
		if (arrayOfPercentK.length < 1){throw new IllegalArgumentException();}
		if (arrayOfPercentD.length < 1){throw new IllegalArgumentException();}
		if (periodAverage > 0 && arrayOfPercentK.length < periodAverage){throw new IllegalArgumentException();}
		if (periodAverage > 0 && arrayOfPercentD.length < periodAverage){throw new IllegalArgumentException();}

		if (periodAverage > 0){
			for (int i=arrayOfPercentK.length-periodAverage; i<arrayOfPercentK.length; i++){
				percentKValue += arrayOfPercentK[i];
			}
			
			for (int i=arrayOfPercentD.length-periodAverage; i<arrayOfPercentD.length; i++){
				percentDValue += arrayOfPercentD[i];
			}
			
			percentKValue /= periodAverage;
			percentDValue /= periodAverage;
			
		}else{
			percentKValue = arrayOfPercentK[arrayOfPercentK.length-1];
			percentDValue = arrayOfPercentK[arrayOfPercentD.length-1];
		}
	}
	
	@Override
	public SignalMetric getSignal(){
		return new SignalMetric(signalMetricType.getNormalizedValue(percentKValue), signalMetricType);
	}
	
	public double getValue(){
		return percentKValue;
	}
}
