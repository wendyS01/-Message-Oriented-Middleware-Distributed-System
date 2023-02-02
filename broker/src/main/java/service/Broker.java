package service;

import org.apache.activemq.ActiveMQConnectionFactory;
import service.message.ClientApplicationMessage;
import service.message.QuotationRequestMessage;
import service.message.QuotationResponseMessage;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

public class Broker {

        /**
         * The broker works as a consumer to listen to the request message from the client on request queue
         * The broker works as a producer to broadcast message to other 3 services on application topic
         * The broker works as a consumer to listen to the other 3 services response message on quotation queue
         * The broker works as a producer to send the message back to the client on response queue
         */

        static Map<Long, ClientApplicationMessage> cache = new HashMap<>();

        public static void main(String[] args) throws JMSException {
                String host = args.length == 0 ? "localhost" : args[0];
                ConnectionFactory factory = new ActiveMQConnectionFactory("failover://tcp://" + host + ":61616");
                Connection connection = factory.createConnection();
                connection.setClientID("broker");
                Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                connection.start();
                //the queue for requests
                Queue requests_queue = session.createQueue("REQUESTS");
                //the queue for responses
                Queue responses_queue = session.createQueue("RESPONSES");
                //the queue for quotations
                Queue quotations_queue = session.createQueue("QUOTATIONS");
                //the topic for application
                Topic applications_topic = session.createTopic("APPLICATIONS");
                //be the consumer of the messages from client
                MessageConsumer consumerForRequests = session.createConsumer(requests_queue);
                //be the producer of the messages sent to the client
                MessageProducer producerForResponses = session.createProducer(responses_queue);
                //be the producer of the messages sent to the 3 services on the application topic
                MessageProducer producerForApplications = session.createProducer(applications_topic);
                //be the consumer on the quotation queue
                MessageConsumer consumerForQuotations = session.createConsumer(quotations_queue);

                Thread thread1 = new Thread(() -> {
                        while (true) {
                                try {
                                        // Get the next client request message from the requests queue
                                        Message messageFromclientRequest = consumerForRequests.receive();
                                        // Check it is the right type of message
                                        if (messageFromclientRequest instanceof ObjectMessage) {
                                                // It’s an Object Message
                                                Object content = ((ObjectMessage) messageFromclientRequest).getObject();
                                                if (content instanceof QuotationRequestMessage) {
                                                        //It’s a QuotationRequestMessage
                                                        QuotationRequestMessage request = (QuotationRequestMessage) content;
                                                        //create the producer
                                                        Message producerQuotationMessage = session.createObjectMessage(request);
                                                        //create ClientApplicationMessage for each client and put it
                                                        // in the cache
                                                        if (!cache.containsKey(request.id)) {
                                                                cache.put(request.id, new ClientApplicationMessage(request.id, request.info));
                                                        }
                                                        //broadcast this quotation message on application topic
                                                        producerForApplications.send(producerQuotationMessage);
                                                        //set the time sleep to wait the broker get the quotations
                                                        Thread.sleep(5000);
                                                        //message sent back to client
                                                        Message messageSentBackClient = session.createObjectMessage(cache.get(request.id));
                                                        producerForResponses.send(messageSentBackClient);
                                                }
                                                //Acknowledge the message is received
                                                messageFromclientRequest.acknowledge();
                                        } else {
                                                System.out.println("Unknown " + "message " + "type: " + messageFromclientRequest.getClass().getCanonicalName());
                                        }
                                } catch (JMSException | InterruptedException e) {
                                        e.printStackTrace();
                                }

                        }
                });
                thread1.start();


                Thread thread2 = new Thread(() -> {
                        while (true) {
                                // Get the next message from the APPLICATION topic
                                try {
                                        Message messageFromQuotationResponse = consumerForQuotations.receive();
                                        // Check it is the right type of message
                                        if (messageFromQuotationResponse instanceof ObjectMessage) {
                                                // It’s an Object Message
                                                Object content = ((ObjectMessage) messageFromQuotationResponse).getObject();
                                                if (content instanceof QuotationResponseMessage) {
                                                        // It’s a Quotation Response Message
                                                        QuotationResponseMessage quotationResponse = (QuotationResponseMessage) content;
                                                        //check if the id of quotationResponse exists in cache
                                                        if (cache.get(quotationResponse.id) != null) {
                                                                //add the quotation from the service into the
                                                                // arraylist of the quotations of the client
                                                                // application message
                                                                ClientApplicationMessage clientApplicationMessage = cache.get(quotationResponse.id);
                                                                clientApplicationMessage.quotations.add(quotationResponse.quotation);
                                                        }
                                                }
                                                //Acknowledge the message is received
                                                messageFromQuotationResponse.acknowledge();
                                        } else {
                                                System.out.println("Unknown " + "message " + "type: " + messageFromQuotationResponse.getClass().getCanonicalName());
                                        }
                                } catch (JMSException e) {
                                        e.printStackTrace();
                                }
                        }
                });
                thread2.start();
        }

}
