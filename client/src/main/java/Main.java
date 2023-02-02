import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.ActiveMQConnectionFactory;

import service.core.ClientInfo;
import service.core.Quotation;
import service.message.ClientApplicationMessage;
import service.message.QuotationRequestMessage;

import javax.jms.*;

public class Main {

        /**
         *The client sends messages to the broker on request queue in one thread
         *The client receives messages from broker on response queue in one thread
         * and display the quotations for each client
         */
        static Map<Long, ClientInfo> cache = new HashMap();
        static long SEED_ID = 0;
        public static void main(String[] args) throws JMSException {
                String host = "localhost";
                ConnectionFactory factory = new ActiveMQConnectionFactory("failover://tcp://"+host+":61616");
                Connection connection = factory.createConnection();
                connection.setClientID("client");
                Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                connection.start();
                //queue for requests
                Queue queueForRequests = session.createQueue("REQUESTS");
                //queue for responses
                Queue queueForResponses = session.createQueue("RESPONSES");
                //be the message producer on the requests queue
                MessageProducer producer = session.createProducer(queueForRequests);
                //be the message consumer on the response queue
                MessageConsumer consumer = session.createConsumer(queueForResponses);

                //thread to get the response message
                Thread threadGetResponse = new Thread(() ->{
                        while(true) {
                                try {
                                        //get the message from the response queue
                                        Message message = consumer.receive();
                                        // Check it is the right type of message
                                        if (message instanceof ObjectMessage) {
                                                // It’s an Object Message
                                                Object content = ((ObjectMessage) message).getObject();
                                                // check if it is client application message
                                                if (content instanceof ClientApplicationMessage) {
                                                        // It’s a client application message
                                                        ClientApplicationMessage response = (ClientApplicationMessage) content;
                                                        ClientInfo info = cache.get(response.id);
                                                        displayProfile(info);
                                                        for(Quotation quotation: response.quotations) {
                                                                displayQuotation(quotation);
                                                        }
                                                        System.out.println("\n");
                                                }
                                                //acknowledge the message have been received
                                                message.acknowledge();
                                        } else {
                                                System.out.println("Unknown message type: " + message.getClass().getCanonicalName());
                                        }
                                } catch (JMSException e) {
                                        e.printStackTrace();
                                }
                        }
                });
                threadGetResponse.start();

                Thread threadSendRequest = new Thread(() ->{
                        System.out.println("Please wait for a moment!");
                        for(ClientInfo client: clients) {
                                try {
                                        //make a quotation request message with the seed_id and client info
                                        QuotationRequestMessage quotationRequest = new QuotationRequestMessage(SEED_ID++, client);
                                        Message request = null;
                                        //make a request message
                                        request = session.createObjectMessage(quotationRequest);
                                        //put the client id and relevant client info in the cache
                                        cache.put(quotationRequest.id, quotationRequest.info);
                                        //send the request message
                                        producer.send(request);
                                } catch (JMSException e) {
                                        e.printStackTrace();
                                }
                        }
                });
                threadSendRequest.start();
        }

        /**
         * Display the client info nicely.
         *
         * @param info
         */
        public static void displayProfile(ClientInfo info) {
                System.out.println("|=================================================================================================================|");
                System.out.println("|                                     |                                     |                                     |");
                System.out.println(
                                "| Name: " + String.format("%1$-29s", info.name) +
                                                " | Gender: " + String.format("%1$-27s", (info.gender==ClientInfo.MALE?"Male":"Female")) +
                                                " | Age: " + String.format("%1$-30s", info.age)+" |");
                System.out.println(
                                "| License Number: " + String.format("%1$-19s", info.licenseNumber) +
                                                " | No Claims: " + String.format("%1$-24s", info.noClaims+" years") +
                                                " | Penalty Points: " + String.format("%1$-19s", info.points)+" |");
                System.out.println("|                                     |                                     |                                     |");
                System.out.println("|=================================================================================================================|");
        }

        /**
         * Display a quotation nicely - note that the assumption is that the quotation will follow
         * immediately after the profile (so the top of the quotation box is missing).
         *
         * @param quotation
         */
        public static void displayQuotation(Quotation quotation) {
                System.out.println(
                                "| Company: " + String.format("%1$-26s", quotation.company) +
                                                " | Reference: " + String.format("%1$-24s", quotation.reference) +
                                                " | Price: " + String.format("%1$-28s", NumberFormat.getCurrencyInstance().format(quotation.price))+" |");
                System.out.println("|=================================================================================================================|");
        }

        /**
         * Test Data
         */
        public static final ClientInfo[] clients = {
                        new ClientInfo("Niki Collier", ClientInfo.FEMALE, 43, 0, 5, "PQR254/1"),
                        new ClientInfo("Old Geeza", ClientInfo.MALE, 65, 0, 2, "ABC123/4"),
                        new ClientInfo("Hannah Montana", ClientInfo.FEMALE, 16, 10, 0, "HMA304/9"),
                        new ClientInfo("Rem Collier", ClientInfo.MALE, 44, 5, 3, "COL123/3"),
                        new ClientInfo("Jim Quinn", ClientInfo.MALE, 55, 4, 7, "QUN987/4"),
                        new ClientInfo("Donald Duck", ClientInfo.MALE, 35, 5, 2, "XYZ567/9")
        };
}
