import io.nats.client.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;


import java.io.*;
import java.util.regex.*;

public class SEC {


    public static void main(String... args) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        String natsURL = "nats://127.0.0.1:4222";
        if (args.length > 0) {
            natsURL = args[0];
        }

        Options options = new Options.Builder().server(natsURL).build();
        Connection nc = Nats.connect(options);

        
            Dispatcher d = nc.createDispatcher((msg) -> {
                String message = new String(msg.getData());
                if(message.contains("<orderReceipt")){
                    // System.out.println(new String(msg.getData()));
                    double stockPrice = parseStockPrice(message);
                    if (stockPrice > 10.0) {
                        try {
                            System.out.println(new String(msg.getData()));
                            logToXML(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            d.subscribe("SEC");
    }

    private static double parseStockPrice(String message) {
        // Extract stock price
        Pattern pattern = Pattern.compile("<total>(.*?)</total>");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0; // 0.0 if not found (shouldn't reach here)
    }

    private static void logToXML(String message) throws Exception {
        String xmlFilePath = "SEClogs/SEC.xml";
    
        File file = new File(xmlFilePath);
    
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(file, true))) {
            writer.println(message);
        }
    
        System.out.println("Logged to XML: " + xmlFilePath);
    }
    
}
