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
	// Name of the trader
	String name;
	// Cash left in the trader's hand
	double cashInHand;
	public double getCashInHand() {
		return cashInHand;
	}

	public void setCashInHand(double cashInHand) {
		this.cashInHand = cashInHand;
	}

	// Stocks owned by the trader
	ArrayList<Order> position;
	// Orders placed by the trader
	ArrayList<Order> ordersPlaced;

	public Trader(String name, double cashInHand) {
		super();
		this.name = name;
		this.cashInHand = cashInHand;
		this.position = new ArrayList<Order>();
		this.ordersPlaced = new ArrayList<Order>();
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
				position.add(stockFromBank);
			}
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
			for (int i = 0; i < ordersPlaced.size(); i++) {
				if (ordersPlaced.get(i).getStockSymbol() == symbol){
					throw new StockMarketExpection("Cannot place multiple orders for the same Stock: " + symbol);
				}
			}
			
			
			if (orderType.equals(OrderType.BUY))
			{
				if (stock.getPrice() * volume > cashInHand) {
					throw new StockMarketExpection("Cannot place order for stock: " + symbol + " since there is not enough money. Trader: " + name);
				}
				BuyOrder order = new BuyOrder(symbol, volume, price, this);
				this.ordersPlaced.add(order);
				m.addOrder(order);
			} else {
				Order tmpOrder = null;
				for (int i = 0; i < position.size(); i++) {
					if (position.get(i).getStockSymbol() == symbol){
						tmpOrder = position.get(i);
					}
				}
				
				if (!OrderUtility.owns(position, symbol)) {
					throw new StockMarketExpection("Cannot sell stock you do not own. Stock: " + symbol);
				}
				else if (tmpOrder.getSize() < volume) {
					throw new StockMarketExpection("Cannot sell more stock than you own. Stock: " + symbol + "Owned: " + volume);
				}
				else {
					SellOrder order = new SellOrder(symbol, volume, price, this);
					this.ordersPlaced.add(order);
					m.addOrder(order);
				}
			}
		}
	}

	public void placeNewMarketOrder(Market m, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		// Similar to the other method, except the order is a market order
		
		Stock stock = m.getStockForSymbol(symbol);
		if (stock != null){
			for (int i = 0; i < ordersPlaced.size(); i++) {
				if (ordersPlaced.get(i).getStockSymbol() == symbol){
					throw new StockMarketExpection("Cannot place multiple orders for the same Stock: " + symbol);
				}
			}
			
			
			if (orderType.equals(OrderType.BUY))
			{
				if (stock.getPrice() * volume > cashInHand) {
					throw new StockMarketExpection("Cannont place order for stock: " + symbol + " since there is not enough money. Trader: " + name);
				}
				BuyOrder order = new BuyOrder(symbol, volume, true, this);
				this.ordersPlaced.add(order);
				m.addOrder(order);
			} else {
				boolean haveStock = false;
				Order tmpOrder = null;
				for (int i = 0; i < position.size(); i++) {
					if (position.get(i).getStockSymbol() == symbol){
						haveStock = true;
						tmpOrder = position.get(i);
					}
				}
				
				if (!haveStock) {
					throw new StockMarketExpection("Cannot sell stock you don not own. Stock: " + symbol);
				}
				else if (tmpOrder.getSize() < volume) {
					throw new StockMarketExpection("Cannot sell more stock than you own. Stock: " + symbol + "Owned: " + volume);
				}
				else {
					SellOrder order = new SellOrder(symbol, volume, true, this);
					this.ordersPlaced.add(order);
					m.addOrder(order);
				}
			}
		}
	}

	public ArrayList<Order> getPosition() {
		return position;
	}

	public void setPosition(ArrayList<Order> position) {
		this.position = position;
	}

	public ArrayList<Order> getOrdersPlaced() {
		return ordersPlaced;
	}

	public void setOrdersPlaced(ArrayList<Order> ordersPlaced) {
		this.ordersPlaced = ordersPlaced;
	}

	public void tradePerformed(Order o, double matchPrice)
			throws StockMarketExpection {
		// Notification received that a trade has been made, the parameters are
		// the order corresponding to the trade, and the match price calculated
		// in the order book. Note than an order can sell some of the stocks he
		// bought, etc. Or add more stocks of a kind to his position. Handle
		// these situations.

		// Update the trader's orderPlaced, position, and cashInHand members
		// based on the notification.
		
		if ( o instanceof BuyOrder){
			if(OrderUtility.owns(this.position, o.getStockSymbol())){
				Order ownedStock = OrderUtility.findAndExtractOrder(this.position, o.getStockSymbol());
				ownedStock.setSize(ownedStock.getSize() + o.getSize());
				this.position.add(ownedStock);
				this.cashInHand -= matchPrice * o.getSize();
				Order order = OrderUtility.findAndExtractOrder(this.ordersPlaced, o.getStockSymbol());
				if (order.getSize() != 0){
					this.ordersPlaced.add(order);
				}
			}
			else{
				this.position.add(o);
				this.cashInHand -= matchPrice * o.getSize();
				Order order = OrderUtility.findAndExtractOrder(this.ordersPlaced, o.getStockSymbol());
				if (order.getSize() != 0){
					this.ordersPlaced.add(order);
				}
			}
		}
		else {
			this.cashInHand += matchPrice * o.getSize();
			OrderUtility.findAndExtractOrder(this.ordersPlaced, o.getStockSymbol());
			Order order = OrderUtility.findAndExtractOrder(this.position, o.getStockSymbol());
			if (order.getSize() != 0){
				this.position.add(order);
			}
		}
	}

	public void printTrader() {
		System.out.println("Trader Name: " + name);
		System.out.println("=====================");
		System.out.println("Cash: " + cashInHand);
		System.out.println("Stocks Owned: ");
		for (Order o : position) {
			o.printStockNameInOrder();
		}
		System.out.println("Stocks Desired: ");
		for (Order o : ordersPlaced) {
			o.printOrder();
		}
		System.out.println("+++++++++++++++++++++");
		System.out.println("+++++++++++++++++++++");
	}
}
