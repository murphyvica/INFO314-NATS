import io.nats.client.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.*;
import java.util.regex.*;

public class SEC {

    public static String[] brokers = {};

    public static void main(String... args) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        String natsURL = "nats://127.0.0.1:4222";
        if (args.length > 0) {
            natsURL = args[0];
        }

        Options options = new Options.Builder().server(natsURL).build();
        Connection nc = Nats.connect(options);

        for (String broker : brokers) {
            Dispatcher d = nc.createDispatcher((msg) -> {
                String message = new String(msg.getData());
                System.out.println(new String(msg.getData()));
                if(message.contains("<orderReceipt")){
                    double stockPrice = parseStockPrice(message);
                    if (stockPrice > 50.0) {
                        try {
                            logToXML(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            d.subscribe("*");
        }
    }

    private static double parseStockPrice(String message) {
        // Extract stock price
        Pattern pattern = Pattern.compile("<adjustedPrice>(.*?)</adjustedPrice>");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0; // 0.0 if not found (shouldn't reach here)
    }

    private static void logToXML(String message) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.newDocument();
        Element rootElement = doc.createElement("logs");
        doc.appendChild(rootElement);

        Element logEntry = doc.createElement("logEntry");
        rootElement.appendChild(logEntry);

        // Create message
        Element logMessage = doc.createElement("message");
        logMessage.appendChild(doc.createTextNode(message));
        logEntry.appendChild(logMessage);

        // Write to XML file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        // Not sure what we should put in file path
        String xmlFilePath = "/SEClogs/SEC.xml";
        StreamResult result = new StreamResult(new File(xmlFilePath));

        // Transform and save the XML file
        transformer.transform(source, result);

        System.out.println("Logged to XML: " + xmlFilePath);
    }
}
