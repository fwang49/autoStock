/**
 * 
 */
package com.autoStock.trading.types;

import java.util.ArrayList;
import java.util.Date;

import com.autoStock.Co;
import com.autoStock.account.BasicAccount;
import com.autoStock.account.TransactionFees;
import com.autoStock.order.OrderDefinitions.OrderMode;
import com.autoStock.order.OrderDefinitions.OrderStatus;
import com.autoStock.order.OrderDefinitions.OrderType;
import com.autoStock.order.OrderStatusListener;
import com.autoStock.position.ListenerOfPositionStatusChange;
import com.autoStock.position.PositionCallback;
import com.autoStock.position.PositionDefinitions.PositionType;
import com.autoStock.position.PositionHistory;
import com.autoStock.position.PositionManager;
import com.autoStock.position.PositionOptions;
import com.autoStock.position.PositionUtils;
import com.autoStock.position.PositionValue;
import com.autoStock.tools.Lock;
import com.autoStock.tools.MathTools;
import com.autoStock.types.Exchange;
import com.autoStock.types.QuoteSlice;
import com.autoStock.types.Symbol;

/**
 * @author Kevin Kowalewski
 * 
 */
public class Position implements OrderStatusListener {
	private final int initialUnits;
	public final Symbol symbol;
	public final Exchange exchange;
	public final BasicAccount basicAccount;
	private double unitPriceFirstKnown;
	private double unitPriceLastKnown;
	public PositionType positionType = PositionType.position_none;
	private ListenerOfPositionStatusChange positionStatusListener;
	private PositionOptions positionOptions;
	private Lock lock = new Lock();
	private final ArrayList<Order> listOfOrder = new ArrayList<Order>();
	private PositionHistory positionHistory = new PositionHistory();

	public Position(PositionType positionType, int units, Symbol symbol, Exchange exchange, double currentPrice, PositionOptions positionOptions, BasicAccount basicAccount, Date dateTime) {
		this.positionType = positionType;
		this.initialUnits = units;
		this.symbol = symbol;
		this.exchange = exchange;
		this.unitPriceFirstKnown = currentPrice;
		this.unitPriceLastKnown = currentPrice;
		this.positionOptions = positionOptions;
		this.basicAccount = basicAccount;
		this.positionHistory.dateOfCreation = dateTime;
	}

	public void setPositionListener(ListenerOfPositionStatusChange positionStatusListener) {
		this.positionStatusListener = positionStatusListener;
	}

	public void executePosition() {
		if (PositionManager.getInstance().orderMode == OrderMode.mode_exchange){
			Co.println("--> Asked to execute " + positionType.name() + ", " + symbol.symbolName);
		}
		synchronized (lock){
			if (PositionManager.getInstance().orderMode == OrderMode.mode_exchange){
				Co.println("--> Synchronized " + positionType.name() + ", " + symbol.symbolName);
			}
			if (initialUnits == 0){
				Co.println("--> Warning initial units are 0!!!");
				positionType = PositionType.position_failed;
			}else{
				PositionUtils positionUtils = new PositionUtils(this, listOfOrder);
				
				if (positionType == PositionType.position_long_entry) {
					Order order = new Order(symbol, exchange, this, OrderType.order_long_entry, initialUnits, unitPriceLastKnown, this);
					order.executeOrder();
					listOfOrder.add(order);
				} else if (positionType == PositionType.position_short_entry) {
					Order order = new Order(symbol, exchange, this, OrderType.order_short_entry, initialUnits, unitPriceLastKnown, this);
					order.executeOrder();
					listOfOrder.add(order);
				} else if (positionType == PositionType.position_long_exit) {
					Order order = new Order(symbol, exchange, this, OrderType.order_long_exit, positionUtils.getOrderUnitsFilled() != 0 ? positionUtils.getOrderUnitsFilled() : initialUnits, unitPriceLastKnown, this);
					order.executeOrder();
				} else if (positionType == PositionType.position_short_exit) {
					Order order = new Order(symbol, exchange, this, OrderType.order_short_exit, positionUtils.getOrderUnitsFilled() != 0 ? positionUtils.getOrderUnitsFilled() : initialUnits, unitPriceLastKnown, this);
					order.executeOrder();
				} else {
					throw new IllegalStateException("Position is already executed... PositionType: " + positionType.name());
				}
			}
		}
	}
	
	public void executeReentry(int units, double currentPrice){
		synchronized(lock){
			if (positionType == PositionType.position_long){
				positionType = PositionType.position_long_entry;
				Order order = new Order(symbol, exchange, this, OrderType.order_long_entry, units, currentPrice, this);
				order.executeOrder();
				synchronized (listOfOrder){
					listOfOrder.add(order);
				}
			}else if (positionType == PositionType.position_short){
				positionType = PositionType.position_short_entry;
				Order order = new Order(symbol, exchange, this, OrderType.order_short_entry, units, currentPrice , this);
				order.executeOrder();
				synchronized (listOfOrder){
					listOfOrder.add(order);
				}
			}else{
				throw new IllegalStateException("Cannot re-enter with position in state: " + positionType.name());
			}
		}
	}
	
	public void cancelEntry(){
		synchronized (lock) {
			if (positionType == PositionType.position_long_entry || positionType == PositionType.position_short_entry){
				positionType = PositionType.position_cancelling; 
			}else{
				throw new IllegalStateException();
			}
		
			synchronized (listOfOrder){
				for (Order order : listOfOrder){
					order.cancelOrder();
				}
			}
			
			positionType = PositionType.position_cancelled;
		}
	}

	public boolean isFilled() {
		if (positionType == PositionType.position_long || positionType == PositionType.position_short) {
			return true;
		}
		return false;
	}

	public double getPositionProfitLossAfterComission(boolean bothComissions) {
		PositionUtils positionUtils = new PositionUtils(this, listOfOrder);
		
		if (positionUtils.getOrderUnitsFilled() == 0){return 0;}
		
		double positionValue = positionUtils.getPositionValueCurrent(false) - positionUtils.getOrderValueIntrinsic(false);
		double comission = 0;
		
		comission += positionUtils.getOrderTransactionFeesIntrinsic();
		if (bothComissions){
			comission += TransactionFees.getTransactionCost(positionUtils.getOrderUnitsIntrinsic(), unitPriceLastKnown);		
		}
		
		return MathTools.round(positionValue - comission);
	}
	
	public double getPositionProfitLossBeforeComission() {
		PositionUtils positionUtils = new PositionUtils(this, listOfOrder);
		
		if (positionUtils.getOrderUnitsFilled() == 0){return 0;}
		
		double positionValue = positionUtils.getPositionValueCurrent(false) - positionUtils.getOrderValueFilled(false);		
		return MathTools.round(positionValue);
	}
	
	public double getCurrentPercentGainLoss(boolean includeEntryTransactionFee){
		PositionUtils positionUtils = new PositionUtils(this, listOfOrder);
		double positionValue = positionUtils.getPositionValueCurrent(false) - positionUtils.getOrderValueIntrinsic(false);
		double comission = 0;
		
		comission += positionUtils.getOrderTransactionFeesIntrinsic();
		comission += TransactionFees.getTransactionCost(positionUtils.getOrderUnitsIntrinsic(), unitPriceLastKnown);		
		
		if (includeEntryTransactionFee){
			positionValue -= comission;
		}
		
		return (positionValue / positionUtils.getOrderValueIntrinsic(false)) * 100;
	}
	
	public double getFirstKnownUnitPrice(){
		return unitPriceFirstKnown;
	}
	
	public double getLastKnownUnitPrice(){
		return unitPriceLastKnown;
	}
	
	public int getInitialUnitsFilled(){
		return initialUnits;
	}
	
	public void updatePosition(QuoteSlice quoteSlice){
		unitPriceLastKnown = quoteSlice.priceClose;
		positionHistory.addProfitLoss(getCurrentPercentGainLoss(true));
	}
	
	public synchronized PositionValue getPositionValue(){
		PositionUtils positionUtils = new PositionUtils(this, listOfOrder);
		PositionValue positionValue = new PositionValue(
			positionUtils.getOrderValueRequested(false), positionUtils.getOrderValueFilled(false), positionUtils.getOrderValueIntrinsic(false),  
			positionUtils.getOrderValueRequested(true), positionUtils.getOrderValueFilled(true), positionUtils.getOrderValueIntrinsic(true), 
			positionUtils.getOrderPriceRequested(true), positionUtils.getOrderPriceFilled(true), positionUtils.getOrderPriceIntrinsic(true),
			positionUtils.getPositionValueCurrent(false), positionUtils.getPositionValueCurrent(true),
			positionUtils.getPositionPriceCurrent(false), positionUtils.getPositionPriceCurrent(true),
			positionUtils.getOrderUnitPriceRequested(), positionUtils.getOrderUnitPriceFilled(), positionUtils.getOrderUnitPriceIntrinsic(),
			unitPriceLastKnown
		);
//		new PositionValueTable().printTable(this, positionValue);
		
		return positionValue;
	}
	
	public PositionUtils getPositionUtils(){
		return new PositionUtils(this, listOfOrder);
	}
	
	public PositionHistory getPositionHistory(){
		return positionHistory;
	}
	
	public double getPositionProfitDrawdown(){
		return MathTools.round(getCurrentPercentGainLoss(true) - positionHistory.getMaxPercentProfitLoss());
	}

	@Override
	public void orderStatusChanged(Order order, OrderStatus orderStatus) {
		PositionUtils positionUtils;
		
		if (PositionManager.getInstance().orderMode == OrderMode.mode_exchange){
			Co.println("--> Received order status change: " + order.symbol.symbolName + ", " + order.orderType.name() + ", " + orderStatus.name());
		}
		
		positionUtils = new PositionUtils(this, listOfOrder);
		
		if (orderStatus == OrderStatus.status_filled) {
			if (order.orderType == OrderType.order_long || order.orderType == OrderType.order_short){
				if (unitPriceFirstKnown == 0){unitPriceFirstKnown = positionUtils.getOrderUnitPriceFilled();}
				if (unitPriceLastKnown == 0){unitPriceLastKnown = positionUtils.getOrderUnitPriceFilled();}
				
				if (positionType != PositionType.position_cancelled && positionType != PositionType.position_cancelling){
					PositionCallback.setPositionSuccess(this);
				}else{
					Co.println("--> Got order success while being canceled...");
				}
			}else if (order.orderType == OrderType.order_long_exited){
				positionType = PositionType.position_long_exited;
			}else if (order.orderType == OrderType.order_short_exited){
				positionType = PositionType.position_short_exited;
			}else{
				throw new IllegalStateException();
			}
			PositionCallback.affectBankBalance(order, PositionManager.getInstance().orderMode, basicAccount, this);
		}else if (orderStatus == OrderStatus.status_cancelled){
			if (listOfOrder.size() > 1){
				synchronized(listOfOrder){
					listOfOrder.remove(order);
				}
			}else{
				positionType = PositionType.position_cancelled;	
			}
		}
		
		positionStatusListener.positionStatusChanged(this);	
		if (positionOptions!= null && positionOptions.listenerOfPositionStatusChange != null){
			positionOptions.listenerOfPositionStatusChange.positionStatusChanged(this);
		}
	}
}
