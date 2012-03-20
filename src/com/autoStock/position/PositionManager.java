/**
 * 
 */
package com.autoStock.position;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;

import com.autoStock.Co;
import com.autoStock.balance.Account;
import com.autoStock.signal.Signal;
import com.autoStock.signal.SignalDefinitions.SignalType;
import com.autoStock.tools.DateTools;
import com.autoStock.trading.types.TypePosition;
import com.autoStock.types.TypeQuoteSlice;

/**
 * @author Kevin Kowalewski
 *
 */
public class PositionManager {
	public static PositionManager instance = new PositionManager();
	private Account account = Account.instance;
	private PositionGenerator positionGenerator = new PositionGenerator(account);
	private ArrayList<TypePosition> listOfPosition = new ArrayList<TypePosition>();
	private int maxPositions = 10;
	
	public void suggestPosition(TypeQuoteSlice typeQuoteSlice, Signal signal){
		//Co.println("Suggested position: " + signal.currentSignalType.name() + " " + typeQuoteSlice.symbol + " @ " + typeQuoteSlice.priceClose + " signal " + signal.getCombinedSignal());
		
		if (getPosition(typeQuoteSlice.symbol) == null && signal.currentSignalType == SignalType.type_buy){
			induceBuy(typeQuoteSlice, signal);
		}else if (getPosition(typeQuoteSlice.symbol) != null && signal.currentSignalType == SignalType.type_sell){
			induceSell(typeQuoteSlice, signal);
		}else if (getPosition(typeQuoteSlice.symbol) == null && signal.currentSignalType == SignalType.type_short){
			induceShort(typeQuoteSlice, signal);
		}else if (signal.currentSignalType == SignalType.type_none){
			if (getPosition(typeQuoteSlice.symbol) != null){
				getPosition(typeQuoteSlice.symbol).lastKnownPrice = typeQuoteSlice.priceClose;
			}
		}else{
			if (getPosition(typeQuoteSlice.symbol) != null){
				getPosition(typeQuoteSlice.symbol).lastKnownPrice = typeQuoteSlice.priceClose;
			}
			//Co.println("Already holding position");
		}
	}
	
	private void induceBuy(TypeQuoteSlice typeQuoteSlice, Signal signal){
		Co.println("Induced buy @ " + DateTools.getPrettyDate(typeQuoteSlice.dateTime));
		TypePosition typePosition = positionGenerator.generatePosition(typeQuoteSlice, signal);
		buyPosition(typePosition);
	}
	
	private void induceSell(TypeQuoteSlice typeQuoteSlice, Signal signal){
		Co.println("Induced sell @ " + DateTools.getPrettyDate(typeQuoteSlice.dateTime));
		sellPosition(getPosition(typeQuoteSlice.symbol), typeQuoteSlice.priceClose, true);
	}
	
	private void induceShort(TypeQuoteSlice typeQuoteSlice, Signal signal){
		Co.println("Induced short @ " + DateTools.getPrettyDate(typeQuoteSlice.dateTime));
		TypePosition typePosition = positionGenerator.generatePosition(typeQuoteSlice, signal);
		buyPosition(typePosition);
	}
	
	public void induceSellAll(){
		Co.println("Induced sell all");
		synchronized(listOfPosition){
			for (TypePosition typePosition : listOfPosition){
				sellPosition(typePosition, typePosition.lastKnownPrice, false);
			}
			
			listOfPosition.clear();
		}
	}
	
	private void buyPosition(TypePosition typePosition){
		synchronized(listOfPosition){
			account.changeBankBalance(-1 * (typePosition.units * typePosition.pricePosition), account.getTransactionCost(typePosition.units));
			listOfPosition.add(typePosition);
			Co.println("Added position: " + typePosition.symbol + "," + typePosition.units + "," + typePosition.pricePosition + " = " + account.getBankBalance());
		}
	}
	
	private void sellPosition(TypePosition typePosition, double price, boolean removeFromList){
		synchronized(listOfPosition){
			account.changeBankBalance(typePosition.units * price, account.getTransactionCost(typePosition.units));
			if (removeFromList){listOfPosition.remove(typePosition);}
			Co.println("Removed position: " + typePosition.symbol + "," + typePosition.units + "," + price + " = " + account.getBankBalance());
		}
	}
	
	private void shortPosition(TypePosition typePosition){
		synchronized (listOfPosition){
			listOfPosition.add(typePosition);
			account.changeBankBalance(-1 * (typePosition.units * typePosition.pricePosition), account.getTransactionCost(typePosition.units));
		}
	}
	
	public TypePosition getPosition(String symbol){
		synchronized(listOfPosition){
			for (TypePosition typePosition : listOfPosition){
				if (typePosition.symbol.equals(symbol)){
					return typePosition;
				}
			}
		}
		
		return null;
	}
}
