package service.message;

import service.core.ClientInfo;
import service.core.Quotation;

import java.io.Serializable;
import java.util.ArrayList;

public class ClientApplicationMessage implements Serializable {
        public long id;
        public ClientInfo clientInfo;
        public ArrayList<Quotation> quotations;
        public ClientApplicationMessage(long id, ClientInfo clientInfo) {
                this.id = id;
                this.clientInfo = clientInfo;
                this.quotations = new ArrayList<>();
        }
}
