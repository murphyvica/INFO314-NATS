import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

import java.util.logging.*;
import io.nats.client.*;
// import org.apache.xmlrpc.XmlRpcResponse;
import java.io.*;
import java.net.*;
import java.net.http.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class StockBrokerClient {
    //Connect to NATS Server
    //Have portfolio of stocks
    //Monitor stocks of interest
    //Have set of conditions for when to buy and sell stocks
    //if conditions are met, sending request to broker to buy/sell
    //Update portfolio
    // String natsURL = "nats://127.0.0.1:4222";
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // Connection nc = Nats.connect(natsURL);
    private String stockBroker;
    Map<String, Integer> stocks;
    List<Strategy> list;

    public class Strategy {
        String stock = "";
        int value = 0;
        int amount = 0;
        String action = "";
        String compare = "";

        public Strategy (String stock, int value, int amount, String action, String compare) {
            this.stock =  stock;
            this.value = value;
            this.amount = amount;
            this.action = action;
            this.compare = compare;
        }
    }

    

    public StockBrokerClient(File strategyFile, File portfolioFile, String stockBroker) {
        this.stockBroker = stockBroker;
        stocks = new HashMap<>();
        parsePortfolioFile(portfolioFile);
        list = new ArrayList<>();
        parseStrategyFile(strategyFile);
    }

    public void prepareRequest(String symbol) throws Exception {
        String natsURL = "nats://127.0.0.1:4222";
        Connection nc = Nats.connect(natsURL);

        
        Dispatcher d = nc.createDispatcher((msg) -> {
            String response = new String(msg.getData());
            System.out.println(response);

            
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(response)));
                Element root = document.getDocumentElement();
                NodeList stockNodes = root.getElementsByTagName("stock");
                // each message contains 1 or multiple messages
                // for (int i = 0; i < stockNodes.getLength(); i++) {
                Element stock = (Element) stockNodes.item(0);
                Element name = (Element) stock.getElementsByTagName("name").item(0);
                Element adjuestedPrice = (Element) stock.getElementsByTagName("adjustedPrice").item(0);
                String stockName = name.getTextContent();
                int price = Integer.parseInt(adjuestedPrice.getTextContent());
                String request = "";
                for (int j = 0; j < list.size(); j++) {
                    Strategy strategy = list.get(j);
                    if (strategy.stock.equals("all") || strategy.stock.equals(stockName)) {
                        if (strategy.compare.equals("below")) {
                            if (price < strategy.value && stocks.get(stockName) >= strategy.amount) {
                                if (strategy.amount == -1) {
                                    request = "<order><" + strategy.action + "symbol=" + stockName + 
                                    "amount=" + stocks.get(stockName) + " /></order>";
                                    break;
                                } else {
                                    request = "<order><" + strategy.action + "symbol=" + stockName + 
                                    "amount=" + strategy.amount + " /></order>";
                                    break;
                                }
                            }
                        } else if (strategy.compare.equals("above")) {
                            if (price > strategy.value && stocks.get(stockName) >= strategy.amount) {
                                if (strategy.amount == -1) {
                                    request = "<order><" + strategy.action + "symbol=" + stockName + 
                                    "amount=" + stocks.get(stockName) + " /></order>";
                                    break;
                                } else {
                                    request = "<order><" + strategy.action + "symbol=" + stockName + 
                                    "amount=" + strategy.amount + " /></order>";
                                    break;
                                }
                            }
                        }
                    }
                } 
                // if (!request.equals("")) {
                //     sendRequest(nc, request);
                // }

            } catch (Exception e) {

            }

        });
        d.subscribe(symbol);
    
    }
        

    public void sendRequest(Connection nc, String requestPayload) throws IOException, InterruptedException{
        // String natsURL = "nats://127.0.0.1:4222";
        // Connection nc = Nats.connect(natsURL);
        // String requestPayload = "<order><buy symbol=\"AMAZ\" amount=\"20\" /></order>";
        try {
            String uniqueReplyTo = nc.createInbox();
            Subscription sub = nc.subscribe(uniqueReplyTo);
            sub.unsubscribe(1);

            // Send the request
            nc.publish("kevin", uniqueReplyTo, requestPayload.getBytes());

            // Read the reply
            Message msg = sub.nextMessage(Duration.ofSeconds(5));

            // Use the response
            // System.out.println(new String(msg.getData()));
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(new String(msg.getData()))));

            Element root = document.getDocumentElement();
            if (root.getElementsByTagName("sell").getLength() > 0) {
                NodeList completeNodes = root.getElementsByTagName("sell");
                Node completeNode = completeNodes.item(0);
                if (completeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element completeElement = (Element) completeNode;
                    String symbol = completeElement.getAttribute("symbol");
                    int amount = Integer.parseInt(completeElement.getAttribute("amount"));
                    int adjustedAmount = 0;
                    if (stocks.get(symbol) == null) {
                        adjustedAmount = amount;
                    } else {
                        adjustedAmount = stocks.get(symbol) - amount;
                    }
                    stocks.put(symbol, adjustedAmount);
                }
            } else if (root.getElementsByTagName("buy").getLength() > 0) {
                NodeList completeNodes = root.getElementsByTagName("buy");
                Node completeNode = completeNodes.item(0);
                if (completeNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element completeElement = (Element) completeNode;
                    String symbol = completeElement.getAttribute("symbol");
                    int amount = Integer.parseInt(completeElement.getAttribute("amount"));
                    int adjustedAmount = 0;
                    if (stocks.get(symbol) == null) {
                        adjustedAmount = amount;
                    } else {
                        adjustedAmount = stocks.get(symbol) + amount;
                    }
                    stocks.put(symbol, adjustedAmount);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    

    public void parsePortfolioFile(File xmlFile) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);

            Element root = document.getDocumentElement();
            NodeList stockNodes = root.getElementsByTagName("stock");

            for (int i = 0; i < stockNodes.getLength(); i++) {
                Node stockNode = stockNodes.item(i);
                if (stockNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element stockElement = (Element) stockNode;
                    String symbol = stockElement.getAttribute("symbol");
                    int shares = Integer.parseInt(stockElement.getTextContent());
                    stocks.put(symbol, shares);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void parseStrategyFile (File xmlFile) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);

            Element root = document.getDocumentElement();
            NodeList strategyNodes = root.getElementsByTagName("when");

            for (int i = 0; i < strategyNodes.getLength(); i++) {
                String stock = "";
                int value = 0;
                int amount = 0;
                String action = "";
                String compare = "";
                Node strategyNode = strategyNodes.item(i);
                Element strategyElement = (Element) strategyNode;
                NodeList stockNode = strategyElement.getElementsByTagName("stock");
                NodeList amountNode = null;
                if (strategyElement.getElementsByTagName("sell").getLength() > 0) {
                    Element amountElement = (Element) strategyElement.getElementsByTagName("sell").item(0);
                    action = "sell";
                    if (amountElement.hasChildNodes()) {
                        amount = Integer.parseInt(amountElement.getTextContent());
                    } else {
                        amount = -1;
                    }
                } else if (strategyElement.getElementsByTagName("buy").getLength() > 0) {
                    Element amountElement = (Element) strategyElement.getElementsByTagName("buy").item(0);
                    action = "buy";
                    if (amountElement.hasChildNodes()) {
                        amount = Integer.parseInt(amountElement.getTextContent());
                    } else {
                        amount = -1;
                    }
                }
                if (strategyElement.getElementsByTagName("below").getLength() > 0) {
                    compare = "below";
                    Element valueElement = (Element) strategyElement.getElementsByTagName("below").item(0);
                    value = Integer.parseInt(valueElement.getTextContent());
                } else if (strategyElement.getElementsByTagName("above").getLength() > 0) {
                    compare = "above";
                    Element valueElement = (Element) strategyElement.getElementsByTagName("above").item(0);
                    value = Integer.parseInt(valueElement.getTextContent());
                    // System.out.println("above");
                    // System.out.println(value);
                }


                if (stockNode.item(0).getNodeType() == Node.ELEMENT_NODE) {
                    Element stockElement = (Element) stockNode.item(0);
                    if (stockElement.hasChildNodes()) {
                        stock = stockElement.getTextContent();
                    } else {
                        stock = "all";
                    }
                }
                // System.out.println(stock + value + amount + action + compare);
                Strategy stra = new Strategy(stock, value, amount, action, compare);
                // System.out.println(stra);
                list.add(stra);
            }
        } catch (Exception e) {
            
        }
    }

    public static void main(String[] args) {
        File portFile = new File("portfolio-1.xml");
        File strFile = new File("strategy-1.xml");
        StockBrokerClient sbc = new StockBrokerClient(strFile, portFile, "kevin");
        // System.out.println(sbc.list);
        // sbc.prepareRequest("APPL");
        try {
            sbc.prepareRequest("APPL");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // while (true) {
        //     sbc.sendRequest();
        // }

    }
}
