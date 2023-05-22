import io.nats.client.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StockBroker {

    String brokerName = "";
    public StockBroker(String brokerName){
        this.brokerName = brokerName;
    }

    public static String[] stocks = ["AMZN", "APPL", "META", "MSFT", "GOOG", "TSLA", "JNJ", "WMT",
                                     "ACTV", "BLIZ", "ROVIO", "NFLX", "ORCL", "CSCO", "NVO", "NVDA",
                                     "GE", "GMC", "FORD", "TM", "DE", "MUFG", "UBER", "ORLY"];
    
    public HashMap<String, double> stockPrices = new HashMap<String, double>();

    public static void main(String... args) throws Exception {
        // Connect to NATS server
        String natsURL = "nats://127.0.0.1:4222";
        if (args.length > 0) {
            natsURL = args[0];
        }

        Options options = new Options.Builder().server(natsURL).build();
        Connection nc = Nats.connect(options);

        for (String stock : stocks) {
            Dispatcher s = nc.createDispatcher((msg) -> {
                String message = new String(msg.getData());
                // Parse message and extract stock symbol and price
                String stockSymbol = parseStockSymbol(message);
                double stockPrice = parseStockPrice(message);

                // Update stockPrices HashMap
                stockPrices.put(stockSymbol, stockPrice);

                // Message for debugging
                System.out.println("Received update for " + stockSymbol + ": " + stockPrice);
            });
            s.subscribe(stock);
        }

        // Keep the program running
        Thread.sleep(Long.MAX_VALUE);

        private static String parseStockSymbol(String message) {
            // Extract stock symbol
            Pattern pattern = Pattern.compile("<name>(.*?)</name>");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null; // Null if not found (shouldn't reach here)
        }

        private static double parseStockPrice(String message) {
            // Extract stock price from
            Pattern pattern = Pattern.compile("<adjustedPrice>(.*?)</adjustedPrice>");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
            return 0.0; // 0.0 if not found (shouldn't reach here)
        }
            // inisialize with multiple stock brokers

    

            //Subscribe to all stocks to get price

            Connection nc = Nats.connect(natsURL);

            
            Dispatcher d = nc.createDispatcher((msg) -> {
                String stockSymbol = "";
                double amount = 0;
                boolean buy = true;
                
                String xmlMessage = new String(msg.getData());
                // Extract stockSymbol using regex
                Pattern symbolPattern = Pattern.compile("symbol=\"(.*?)\"");
                Matcher symbolMatcher = symbolPattern.matcher(xmlMessage);
                if (symbolMatcher.find()) {
                    stockSymbol = symbolMatcher.group(1);
                }
                
                // Extract amount using regex
                //Pattern amountPattern = Pattern.compile("amount=\"(.*?)\"");
                //Matcher amountMatcher = amountPattern.matcher(xmlMessage);
                //if (amountMatcher.find()) {
                //    amount = Integer.parseInt(amountMatcher.group(1));
                //}

                amount = stockPrices.get(stockSymbol);
                
                // Check if it's a buy or sell order
                buy = xmlMessage.contains("<buy");

                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    // Handle the exception
                }

                // Perform the buy or sell operation based on the 'buy' variable
                double transactionAmount = calculateTransactionAmount(amount, buy);

                // Respond to NATS message with the following structure:
                // <orderReceipt><(original msg here)><complete amount="amount" /></orderReceipt>
                String orderReceipt = createOrderReceipt(msg.getData(), transactionAmount);

                // Publish the order receipt
                nc.publish(msg.getReplyTo(), orderReceipt.getBytes());
            });

            // Subscribe to the NATS subject "clientName"
            d.subscribe("kevin");
        }


        private static double calculateTransactionAmount(doubble amount, boolean buy) {
            // Logic for adding the Broker's fee
            // Take total price of amount since amount is number of shares
            double transactionAmount = 0.0;
            if (buy) {
                transactionAmount = amount + (amount * (10.0 / 100.0));
            } else {
                transactionAmount = amount - (amount * (10.0 / 100.0));
            }
            return transactionAmount;
        }

        private static String createOrderReceipt(byte[] originalMsg, double transactionAmount) {
            String originalMsgStr = new String(originalMsg);
            
            // Create the order receipt XML using the original message and transaction amount
            String orderReceipt = "<orderReceipt>" +
                                    originalMsgStr +
                                    "<complete amount=\"" + transactionAmount + "\" />" +
                                "</orderReceipt>";
            return orderReceipt;
        }
}