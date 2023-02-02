package service;

import org.apache.activemq.ActiveMQConnectionFactory;
import service.core.Quotation;
import service.core.QuotationService;
import service.dodgydrivers.DDQService;
import service.message.QuotationRequestMessage;
import service.message.QuotationResponseMessage;

import javax.jms.*;
/**
 * This is dodgydriver receiver，it will be a consumer to listen to
 * the messages from broker on the application topic
 * and generate a quotation and send a quotation response message on the quotation queue
 *
 */
public class Receiver {
        static QuotationService service = new DDQService();
        public static void main(String[] args) throws JMSException {
                String host = args.length == 0 ? "localhost":args[0];
                //build the connection factory on TCP of ActiveMQ
                ConnectionFactory factory = new ActiveMQConnectionFactory("failover://tcp://"+host+":61616");
                Connection connection = factory.createConnection();
                connection.setClientID("dodgydrivers");
                Session session = ((Connection) connection).createSession(false, Session.CLIENT_ACKNOWLEDGE);
                //the queue for qutations
                Queue queue = session.createQueue("QUOTATIONS");
                //the topic for applications
                Topic topic = session.createTopic("APPLICATIONS");
                // be the message consumer on the topic application
                MessageConsumer consumer = session.createConsumer(topic);
                // be the message producer on the quotations queue
                MessageProducer producer = session.createProducer(queue);
                //call tells the messaging provider to start message delivery
                connection.start();
                while (true) {
                        // Get the next message from the APPLICATION topic
                        Message message = consumer.receive();
                        // Check it is the right type of message
                        if (message instanceof ObjectMessage) {
                                // It’s an Object Message
                                Object content = ((ObjectMessage) message).getObject();
                                if (content instanceof QuotationRequestMessage) {
                                        // It’s a Quotation Request Message
                                        QuotationRequestMessage request = (QuotationRequestMessage) content;
                                        // Generate a quotation and send a quotation response message…
                                        Quotation quotation = service.generateQuotation(request.info);
                                        Message response = session.createObjectMessage(new QuotationResponseMessage(request.id, quotation));
                                        producer.send(response);
                                }
                        } else {
                                System.out.println("Unknown message type: " + message.getClass().getCanonicalName());
                        }
                }
        }
}
