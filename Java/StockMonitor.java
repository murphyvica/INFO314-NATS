import java.io.IOException;
import java.io.StringReader;

import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;

import io.nats.client.*;

public class StockMonitor {
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
        /* } else {
            Dispatcher d1 = nc.createDispatcher((msg) -> {
                System.out.println(new String(msg.getData()));
                System.out.println("Received");
                //logStockData(msg.getData());
            });
    
            d1.subscribe("*");

        } */
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

        for (int i = 0; i < nodeList2.getLength(); i++) {
            Node n2 = nodeList2.item(i);
            Element e2 = (Element) n2;
            String change = e2.getElementsByTagName("adjustment").item(0).getTextContent();
            System.out.println(change);
            String price = e2.getElementsByTagName("adjustedPrice").item(0).getTextContent();
            System.out.println(price);
        }



    } catch (Exception ex) {
        ex.printStackTrace();
    }
}
}

