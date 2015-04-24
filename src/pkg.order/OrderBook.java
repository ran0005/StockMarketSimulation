package pkg.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Collections;
import java.util.Map.Entry;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.market.api.PriceSetter;

public class OrderBook {
	Market m;
	HashMap<String, ArrayList<Order>> buyOrders;
	public HashMap<String, ArrayList<Order>> getBuyOrders() {
		return buyOrders;
	}

	public void setBuyOrders(HashMap<String, ArrayList<Order>> buyOrders) {
		this.buyOrders = buyOrders;
	}

	public HashMap<String, ArrayList<Order>> getSellOrders() {
		return sellOrders;
	}

	public void setSellOrders(HashMap<String, ArrayList<Order>> sellOrders) {
		this.sellOrders = sellOrders;
	}
	HashMap<String, ArrayList<Order>> sellOrders;

	public OrderBook(Market m) {
		this.m = m;
		buyOrders = new HashMap<String, ArrayList<Order>>();
		sellOrders = new HashMap<String, ArrayList<Order>>();
	}

	public void addToOrderBook(Order order) {
		// Populate the buyOrders and sellOrders data structures, whichever
		// appropriate
		if (order instanceof BuyOrder) {
			if (buyOrders.containsKey(order.getStockSymbol())) {
				buyOrders.get(order.getStockSymbol()).add(order);
			}
			else {
				ArrayList<Order> list = new ArrayList<Order>();
				list.add(order);
				buyOrders.put(order.getStockSymbol(), list);
			}
		}
		else {
			if (sellOrders.containsKey(order.getStockSymbol())) {
				sellOrders.get(order.getStockSymbol()).add(order);
			}
			else {
				ArrayList<Order> list = new ArrayList<Order>();
				list.add(order);
				sellOrders.put(order.getStockSymbol(), list);
			}
		}
	}

	public void trade() {
		// Complete the trading.
		// 1. Follow and create the orderbook data representation (see spec)
		// 2. Find the matching price
		// 3. Update the stocks price in the market using the PriceSetter.
		// Note that PriceSetter follows the Observer pattern. Use the pattern.
		// 4. Remove the traded orders from the orderbook
		// 5. Delegate to trader that the trade has been made, so that the
		// trader's orders can be placed to his possession (a trader's position
		// is the stocks he owns)
		// (Add other methods as necessary)
		
		for(Entry<String, ArrayList<Order>> buyList : buyOrders.entrySet()) {
			for ( Entry<String, ArrayList<Order>> sellList : sellOrders.entrySet()) {
				if (buyList.getKey() == sellList.getKey()) {
					ArrayList<Order> buySorted = buyOrderSort(buyList.getValue());
					ArrayList<Order> sellSorted = sellOrderSort(sellList.getValue());
					
					int buyNum = 0;
					int sellNum = 0;
					double price = 0.0;
					int sellVolume = 0;
					
					ArrayList<Double> values = findMarketPrice(buySorted, sellSorted);
					
					buyNum = values.get(0).intValue();
					sellNum = values.get(1).intValue();
					price = values.get(2);
					sellVolume = values.get(3).intValue();
					
					if (buyNum != -1 && sellNum != -1 && price != -1.0){
						PriceSetter set = new PriceSetter();
        				set.registerObserver(m.getMarketHistory());
        				m.getMarketHistory().setSubject(set);
        				set.setNewPrice(m, (String)buyList.getKey(), price);
        				
        				for (int i = 0; i <= buyNum; i++) {
        					Order o1 = buySorted.get(i);
        					if (o1.getSize() <= sellVolume) {
        						buyOrders.get(o1.getStockSymbol()).remove(o1);
        						sellVolume -= o1.getSize();
        					} else {
        						o1.setSize(o1.getSize() - sellVolume);
        					}
        					
        					try {
								o1.getTrader().tradePerformed(o1, price);
							} catch (StockMarketExpection e) {
								e.printStackTrace();
							}
        				}
        				
        				for (int i = 0; i <= sellNum; i++) {
        					Order o2 = sellSorted.get(i);
        					sellOrders.get(o2.getStockSymbol()).remove(o2);
        					try {
								o2.getTrader().tradePerformed(o2, price);
							} catch (StockMarketExpection e) {
								e.printStackTrace();
							}
        				}
					}
				}
			}
		}
	}
	
	private ArrayList<Double> findMarketPrice(ArrayList<Order> buyList, ArrayList<Order> sellList) {
		ArrayList<Double> values = new ArrayList<Double>();
		int buyVolume = 0;
		int sellVolume = 0;
		int finalBuyVolume = 10000000;
		int finalSellVolume = 0;
		int totalSellVolume = -1;
		int buyNumber = -1, sellNumber = -1;
		double price = -1.0;
		
		for (int i = 0; i < buyList.size(); i++){
			buyVolume += buyList.get(i).getSize();
			sellVolume = 0;
			for (int j = 0; j < sellList.size(); j++) {
				sellVolume += sellList.get(j).getSize();
				if (buyVolume >= sellVolume) {
					if ((buyVolume - sellVolume) <= (finalBuyVolume - finalSellVolume) && totalSellVolume < sellVolume && buyList.get(i).getPrice() >= sellList.get(j).getPrice()) {
						buyNumber = i;
						sellNumber = j;
						finalBuyVolume = buyVolume;
						finalSellVolume = sellVolume;
						price = sellList.get(sellNumber).getPrice();
						totalSellVolume = sellVolume;
					}
				}
			}
		}
		values.add((double) buyNumber);
		values.add((double) sellNumber);
		values.add(price);
		values.add((double) totalSellVolume);
		return values;
	}
	
	private ArrayList<Order> buyOrderSort(ArrayList<Order> buyOrders){
		ArrayList<Order> sorted = new ArrayList<Order>();
		ArrayList<Order> unsorted = (ArrayList<Order>) buyOrders.clone();
		
		for (int i = 0; i < buyOrders.size(); i++){
			if(buyOrders.get(i).getPrice() == 0){
				sorted.add(buyOrders.get(i));
				unsorted.remove(i);
			}
		}
		
		Collections.sort(unsorted, new Comparator<Order>() {
			@Override
			public int compare(Order o1, Order o2){
				return Double.compare(o2.getPrice(), o1.getPrice());
			}
		});
		

		sorted.addAll(unsorted);
		
		return sorted;
	}
	private ArrayList<Order> sellOrderSort(ArrayList<Order> buyOrders){
		ArrayList<Order> sorted = (ArrayList<Order>) buyOrders.clone();
		Collections.sort(sorted, new Comparator<Order>() {
			@Override
			public int compare(Order o1, Order o2){
				return Double.compare(o1.getPrice(), o2.getPrice());
			}
		});
	
		return sorted;
	}

}
