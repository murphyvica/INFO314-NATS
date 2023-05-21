import io.nats.client.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StockBroker {

    public static void main(String... args) throws Exception {
        // Connect to NATS server
        String natsURL = "nats://127.0.0.1:4222";
        if (args.length > 0) {
            natsURL = args[0];
        }

        Connection nc = Nats.connect(natsURL);
        
        Dispatcher d = nc.createDispatcher((msg) -> {
            String stockSymbol = "";
            int amount = 0;
            boolean buy = true;
            
            String xmlMessage = new String(msg.getData());
            // Extract stockSymbol using regex
            Pattern symbolPattern = Pattern.compile("symbol=\"(.*?)\"");
            Matcher symbolMatcher = symbolPattern.matcher(xmlMessage);
            if (symbolMatcher.find()) {
                stockSymbol = symbolMatcher.group(1);
            }
            
            // Extract amount using regex
            Pattern amountPattern = Pattern.compile("amount=\"(.*?)\"");
            Matcher amountMatcher = amountPattern.matcher(xmlMessage);
            if (amountMatcher.find()) {
                amount = Integer.parseInt(amountMatcher.group(1));
            }
            
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
        d.subscribe("clientName");
    }

    private static double calculateTransactionAmount(int amount, boolean buy) {
        // Logic for adding the Broker's fee
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
