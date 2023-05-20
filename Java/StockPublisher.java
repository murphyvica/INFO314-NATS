import io.nats.client.*;
import java.time.Instant;

/**
 * Take the NATS URL on the command-line.
 */
public class StockPublisher {
    private static Connection nc;
  public static void main(String... args) throws Exception {
      String natsURL = "nats://127.0.0.1:4222";
      if (args.length > 0) {
          natsURL = args[0];
      }

      nc = Nats.connect(natsURL);

      System.console().writer().println("Starting stock publisher....");

      StockMarket sm1 = new StockMarket(StockPublisher::publishMessage, "AMZN", "APPL", "META", "MSFT", "GOOG", "TSLA", "JNJ", "WMT");
      new Thread(sm1).start();
      StockMarket sm2 = new StockMarket(StockPublisher::publishMessage, "ACTV", "BLIZ", "ROVIO", "NFLX", "ORCL", "CSCO", "NVO", "NVDA");
      new Thread(sm2).start();
      StockMarket sm3 = new StockMarket(StockPublisher::publishMessage, "GE", "GMC", "FORD", "TM", "DE", "MUFG", "UBER", "ORLY");
      new Thread(sm3).start();
    }

    public synchronized static void publishDebugOutput(String symbol, int adjustment, int price) {
        System.console().writer().printf("PUBLISHING %s: %d -> %f\n", symbol, adjustment, (price / 100.f));
    }
    // When you have the NATS code here to publish a message, put "publishMessage" in
    // the above where "publishDebugOutput" currently is
    public synchronized static void publishMessage(String symbol, int adjustment, int price) {
        //NATS code here to publish stuff to NATS server instead of console
        Instant timestamp = Instant.now();
        String message = "<message sent=\"" + timestamp + "><stock><name>" + symbol + "</name><adjustment>" + adjustment + "</adjustment>"
                        + "<adjustedPrice>" + price / 100.f + "</adjustedPrice></stock></message>";
        System.console().writer().println(message);
        nc.publish(symbol, message.getBytes());
    } 
}