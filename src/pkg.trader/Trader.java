package pkg.trader;

import java.util.ArrayList;

import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.order.BuyOrder;
import pkg.order.Order;
import pkg.order.OrderType;
import pkg.order.SellOrder;
import pkg.stock.Stock;
import pkg.util.OrderUtility;

public class Trader {
	String name;
	double cashInHand;
	public double getCashInHand() {
		return cashInHand;
	}

	public void setCashInHand(double cashInHand) {
		this.cashInHand = cashInHand;
	}

	ArrayList<Order> stocksOwnedByTrader;
	ArrayList<Order> ordersPlacedByTrader;

	public Trader(String name, double cashInHand) {
		super();
		this.name = name;
		this.cashInHand = cashInHand;
		this.stocksOwnedByTrader = new ArrayList<Order>();
		this.ordersPlacedByTrader = new ArrayList<Order>();
	}

	public void buyFromBank(Market m, String symbol, int volume)
			throws StockMarketExpection {
		// Buy stock straight from the bank
		// Need not place the stock in the order list
		// Add it straight to the user's position
		// If the stock's price is larger than the cash possessed, then an
		// exception is thrown
		// Adjust cash possessed since the trader spent money to purchase a
		// stock.
		Stock stock = m.getStockForSymbol(symbol);
		if (stock != null){
			if (stock.getPrice() * volume > cashInHand) {
				throw new StockMarketExpection("Cannot place order for stock: " + symbol + " since there is not enough money. Trader: " + name);
			}else {
				cashInHand -= volume * stock.getPrice();
				BuyOrder stockFromBank = new BuyOrder(symbol, volume, stock.getPrice(), this);
				stocksOwnedByTrader.add(stockFromBank);
			}
		}
		else {
			throw new StockMarketExpection("Invalid Stock: " + symbol);
		}
	}

	public void placeNewOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		// Place a new order and add to the orderlist
		// Also enter the order into the orderbook of the market.
		// Note that no trade has been made yet. The order is in suspension
		// until a trade is triggered.
		//
		// If the stock's price is larger than the cash possessed, then an
		// exception is thrown
		// A trader cannot place two orders for the same stock, throw an
		// exception if there are multiple orders for the same stock.
		// Also a person cannot place a sell order for a stock that he does not
		// own. Or he cannot sell more stocks than he possesses. Throw an
		// exception in these cases.
		
		Stock stock = m.getStockForSymbol(symbol);
		if (stock != null){
			for (int i = 0; i < ordersPlacedByTrader.size(); i++) {
				if (ordersPlacedByTrader.get(i).getStockSymbol() == symbol){
					throw new StockMarketExpection("Cannot place multiple orders for the same Stock: " + symbol);
				}
			}
			
			
			if (orderType.equals(OrderType.BUY))
			{
				if (stock.getPrice() * volume > cashInHand) {
					throw new StockMarketExpection("Cannot place order for stock: " + symbol + " since there is not enough money. Trader: " + name);
				}
				BuyOrder order = new BuyOrder(symbol, volume, price, this);
				this.ordersPlacedByTrader.add(order);
				m.addOrder(order);
			} else {
				Order tmpOrder = null;
				for (int i = 0; i < stocksOwnedByTrader.size(); i++) {
					if (stocksOwnedByTrader.get(i).getStockSymbol() == symbol){
						tmpOrder = stocksOwnedByTrader.get(i);
					}
				}
				
				checkForError(symbol, volume, tmpOrder);
				SellOrder order = new SellOrder(symbol, volume, price, this);
				this.ordersPlacedByTrader.add(order);
				m.addOrder(order);
			}
		}
		else {
			throw new StockMarketExpection("Invalid Stock: " + symbol);
		}
	}

	private void checkForError(String symbol, int volume, Order tmpOrder)
			throws StockMarketExpection {
		if (!OrderUtility.owns(stocksOwnedByTrader, symbol)) {
			throw new StockMarketExpection("Cannot sell stock you do not own. Stock: " + symbol);
		}
		else if (tmpOrder.getSize() < volume) {
			throw new StockMarketExpection("Cannot sell more stock than you own. Stock: " + symbol + "Owned: " + volume);
		}
	}

	public void placeNewMarketOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		// Similar to the other method, except the order is a market order
		
		Stock stock = m.getStockForSymbol(symbol);
		if (stock != null){
			for (int i = 0; i < ordersPlacedByTrader.size(); i++) {
				if (ordersPlacedByTrader.get(i).getStockSymbol() == symbol){
					throw new StockMarketExpection("Cannot place multiple orders for the same Stock: " + symbol);
				}
			}
			
			
			if (orderType.equals(OrderType.BUY))
			{
				if (stock.getPrice() * volume > cashInHand) {
					throw new StockMarketExpection("Cannont place order for stock: " + symbol + " since there is not enough money. Trader: " + name);
				}
				BuyOrder order = new BuyOrder(symbol, volume, true, this);
				this.ordersPlacedByTrader.add(order);
				m.addOrder(order);
			} else {
				boolean haveStock = false;
				Order tmpOrder = null;
				for (int i = 0; i < stocksOwnedByTrader.size(); i++) {
					if (stocksOwnedByTrader.get(i).getStockSymbol() == symbol){
						haveStock = true;
						tmpOrder = stocksOwnedByTrader.get(i);
					}
				}
				
				checkForError(symbol, volume, tmpOrder);
				SellOrder order = new SellOrder(symbol, volume, true, this);
				this.ordersPlacedByTrader.add(order);
				m.addOrder(order);
			}
		}
		else {
			throw new StockMarketExpection("Invalid Stock: " + symbol);
		}
	}

	public ArrayList<Order> getPosition() {
		return stocksOwnedByTrader;
	}

	public void setPosition(ArrayList<Order> position) {
		this.stocksOwnedByTrader = position;
	}

	public ArrayList<Order> getOrdersPlaced() {
		return ordersPlacedByTrader;
	}

	public void setOrdersPlaced(ArrayList<Order> ordersPlaced) {
		this.ordersPlacedByTrader = ordersPlaced;
	}

	public void tradePerformed(Order order, double matchPrice)
			throws StockMarketExpection {
		// Notification received that a trade has been made, the parameters are
		// the order corresponding to the trade, and the match price calculated
		// in the order book. Note than an order can sell some of the stocks he
		// bought, etc. Or add more stocks of a kind to his position. Handle
		// these situations.

		// Update the trader's orderPlaced, position, and cashInHand members
		// based on the notification.
		
		if ( order instanceof BuyOrder){
			if(OrderUtility.owns(this.stocksOwnedByTrader, order.getStockSymbol())){
				Order ownedStock = OrderUtility.findAndExtractOrder(this.stocksOwnedByTrader, order.getStockSymbol());
				ownedStock.setSize(ownedStock.getSize() + order.getSize());
				this.stocksOwnedByTrader.add(ownedStock);
				this.cashInHand -= matchPrice * order.getSize();
				Order newOrder = OrderUtility.findAndExtractOrder(this.ordersPlacedByTrader, order.getStockSymbol());
				if (newOrder.getSize() != 0){
					this.ordersPlacedByTrader.add(newOrder);
				}
			}
			else{
				this.stocksOwnedByTrader.add(order);
				this.cashInHand -= matchPrice * order.getSize();
				Order newOrder = OrderUtility.findAndExtractOrder(this.ordersPlacedByTrader, order.getStockSymbol());
				if (newOrder.getSize() != 0){
					this.ordersPlacedByTrader.add(newOrder);
				}
			}
		}
		else {
			this.cashInHand += matchPrice * order.getSize();
			OrderUtility.findAndExtractOrder(this.ordersPlacedByTrader, order.getStockSymbol());
			Order newOrder = OrderUtility.findAndExtractOrder(this.stocksOwnedByTrader, order.getStockSymbol());
			if (newOrder.getSize() != 0){
				this.stocksOwnedByTrader.add(newOrder);
			}
		}
	}

	public void printTrader() {
		System.out.println("Trader Name: " + name);
		System.out.println("=====================");
		System.out.println("Cash: " + cashInHand);
		System.out.println("Stocks Owned: ");
		for (Order o : stocksOwnedByTrader) {
			o.printStockNameInOrder();
		}
		System.out.println("Stocks Desired: ");
		for (Order o : ordersPlacedByTrader) {
			o.printOrder();
		}
		System.out.println("+++++++++++++++++++++");
		System.out.println("+++++++++++++++++++++");
	}
}