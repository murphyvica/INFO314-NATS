import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;

import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import io.nats.client.*;

public class StockMonitor {

    public static final String[] stocks = {"AMZN", "APPL", "META", "MSFT", "GOOG", "TSLA", "JNJ", "WMT",
    "ACTV", "BLIZ", "ROVIO", "NFLX", "ORCL", "CSCO", "NVO", "NVDA",
    "GE", "GMC", "FORD", "TM", "DE", "MUFG", "UBER", "ORLY"};
    public static void main(String... args) throws IOException, InterruptedException{
        Connection nc = Nats.connect("nats://localhost:4222");

        if (args.length > 0) {
            for (String stock : args) {
            Dispatcher d1 = nc.createDispatcher((msg) -> {
                System.out.println(new String(msg.getData()));
                System.out.println("Received");
                logStockData(new String(msg.getData()));
            });
    
            d1.subscribe(stock);

        }
        } else {
            Dispatcher d1 = nc.createDispatcher((msg) -> {
                System.out.println(new String(msg.getData()));
                System.out.println("Received");
                //logStockData(msg.getData());
            });
    
            for (String s : stocks) {
                d1.subscribe(s);
            }

        }
    }

    private static void logStockData(String data) {
        try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.parse(new InputSource(new StringReader(data)));

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        // Parsing Timestamp

        String expression = "/message";	        
        NodeList nodeList = (NodeList) xpath.compile(expression).evaluate(doc, XPathConstants.NODESET);

        Node n = nodeList.item(0);
        Element e = (Element) n;
        String time = e.getAttribute("sent");
        System.out.println(time);

        // Parsing Price data

        String expression2 = "/message/stock";
        NodeList nodeList2 = (NodeList) xpath.compile(expression2).evaluate(doc, XPathConstants.NODESET);

        Node n2 = nodeList2.item(0);
        Element e2 = (Element) n2;
        String name = e2.getElementsByTagName("name").item(0).getTextContent();
        System.out.println(name);
        String change = e2.getElementsByTagName("adjustment").item(0).getTextContent();
        System.out.println(change);
        String price = e2.getElementsByTagName("adjustedPrice").item(0).getTextContent();
        System.out.println(price);

        // Logging to file

        String s = Paths.get(".").toAbsolutePath().normalize().toString();
        File f = new File(s + "/logs/" + name + "-price.log");
        if (!f.exists()) {
            f.createNewFile();
        }
        FileWriter myWriter = new FileWriter(f, true);
        myWriter.write(time + ", " + change + ", " + price);
        myWriter.close();


    } catch (Exception ex) {
        ex.printStackTrace();
    }
}
}
