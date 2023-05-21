import java.io.*;
import io.nats.client.*;

public class StockBrokerClient {
    //Connect to NATS Server

    Connection nc = Nats.connect("nats://localhost:4222");

    //Have portfolio of stocks
    //Monitor stocks of interest
    //Have set of conditions for when to buy and sell stocks
    //if conditions are met, sending request to broker to buy/sell
    //Update portfolio
}
