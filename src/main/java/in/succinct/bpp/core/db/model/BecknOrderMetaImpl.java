package in.succinct.bpp.core.db.model;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.plugins.collab.db.model.user.Phone;
import in.succinct.beckn.Agent;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.Contact;
import in.succinct.beckn.Context;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Fulfillment.FulfillmentStatus;
import in.succinct.beckn.FulfillmentStop;
import in.succinct.beckn.FulfillmentStops;
import in.succinct.beckn.Invoice;
import in.succinct.beckn.Message;
import in.succinct.beckn.Order;
import in.succinct.beckn.Order.Status;
import in.succinct.beckn.Payment;
import in.succinct.beckn.Payment.PaymentStatus;
import in.succinct.beckn.Payment.PaymentTransaction;
import in.succinct.beckn.Payment.PaymentTransaction.PaymentTransactions;
import in.succinct.beckn.PaymentMethod;
import in.succinct.beckn.Provider.Directories;
import in.succinct.beckn.Request;
import in.succinct.beckn.SellerException;
import in.succinct.beckn.Subscriber;
import in.succinct.bpp.core.util.NetworkManager;
import in.succinct.events.PaymentStatusEvent;
import org.json.simple.JSONObject;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class BecknOrderMetaImpl extends ModelImpl<BecknOrderMeta> {

    public BecknOrderMetaImpl(BecknOrderMeta meta){
        super(meta);
    }
    public BecknObject getStatusAudit(){
        String json = getProxy().getStatusUpdatedAtJson();
        if (json == null){
            json = "{}";
        }
        BecknObject object = new BecknObject(json);
        return object;
    }

    public Date getStatusReachedAt(Status status){
        BecknObject sa = getStatusAudit();
        return sa.getTimestamp(status.toString());
    }
    public void setStatusReachedAt(Status status, Date at){
        BecknObject sa = getStatusAudit();
        Date statusReachedAt = sa.getTimestamp(status.toString());
        if (statusReachedAt == null || at.before(statusReachedAt)) {
            sa.set(status.toString(), at, BecknObject.TIMESTAMP_FORMAT_WITH_MILLS);
            getProxy().setStatusUpdatedAtJson(sa.getInner().toString());
        }
    }

    public Date getFulfillmentStatusReachedAt(FulfillmentStatus status){
        BecknObject sa = getStatusAudit();
        BecknObject fsa = sa.get(BecknObject.class,"fulfillmentStatusAudit",true);
        return fsa.getTimestamp(status.toString());
    }
    public void setFulfillmentStatusReachedAt(FulfillmentStatus status, Date at){
        BecknObject sa = getStatusAudit();
        BecknObject fsa = sa.get(BecknObject.class,"fulfillmentStatusAudit",true);

        Date statusReachedAt = fsa.getTimestamp(status.toString());
        if (statusReachedAt == null || at.before(statusReachedAt)) {
            fsa.set(status.toString(), at, BecknObject.TIMESTAMP_FORMAT_WITH_MILLS);
            getProxy().setStatusUpdatedAtJson(sa.getInner().toString());
        }
    }
    
    
    public void summarize(boolean force){
        BecknOrderMeta m = getProxy();
        
        Order becknOrder = new Order(m.getOrderJson());
        
        in.succinct.bpp.core.db.model.Order order = Database.getTable(in.succinct.bpp.core.db.model.Order.class).newRecord();
        order.setBecknOrderMetaId(m.getId());
        order = Database.getTable(in.succinct.bpp.core.db.model.Order.class).getRefreshed(order);
        if (!order.getRawRecord().isNewRecord() && !force){
            return;
        }
        
        order.setOrderId(becknOrder.getId());
        order.setTransactionId(m.getBecknTransactionId());
        
        String logisticsTransactionId = becknOrder.getFulfillment().getTag("delivery_order","transaction_id");
        String logisticsOrderId = becknOrder.getFulfillment().getTag("delivery_order","order_id");
        String selfManaged = becknOrder.getFulfillment().getTag("delivery_order","self_managed");
        order.setLogisticsOrderId(logisticsOrderId);
        order.setLogisticsTransactionId(logisticsTransactionId);
        order.setLogisticsSelfManaged(order.getReflector().getJdbcTypeHelper().getTypeRef(boolean.class).getTypeConverter().valueOf(selfManaged));
        
        order.setEnvironment(becknOrder.getProvider().getTag("network","environment"));
        Directories directories  =becknOrder.getProvider().getDirectories();
        
        if (directories.isEmpty()){
            order.setMarketedVia("Self");
        }else {
            Descriptor descriptor = directories.get(0).getDescriptor();
            if (descriptor == null || ObjectUtil.isVoid(descriptor.getName())){
                order.setMarketedVia("Self");
            }else {
                order.setMarketedVia(descriptor.getName());
            }
        }
        
        FulfillmentStops stops = becknOrder.getFulfillment().getFulfillmentStops();
        if (stops.size() > 1){
            FulfillmentStop stop = stops.get(stops.size()-1);
            order.setCustomerAddress(stop.getLocation().get("address"));
            order.setCity(stop.getLocation().getCity().getName());
            order.setPinCode(stop.getLocation().getPinCode());
            order.setPhoneNumber(stop.getContact() == null ? null : stop.getContact().getPhone());
            
        }else if (becknOrder.getBilling() != null){
            order.setCustomerAddress(becknOrder.getBilling().get("address"));
            order.setCity(becknOrder.getBilling().getCity().getName());
            order.setPinCode(becknOrder.getBilling().getPinCode());
            order.setPhoneNumber(becknOrder.getBilling().getPhone());
        }
        
        order.setFullfilledAt(m.getReflector().getJdbcTypeHelper().getTypeRef(Timestamp.class).getTypeConverter().toStringISO(m.getStatusReachedAt(Status.Completed)));
        order.setOrderCreatedAt(m.getReflector().getJdbcTypeHelper().getTypeRef(Timestamp.class).getTypeConverter().toStringISO(m.getCreatedAt()));
        
        order.setStatus(becknOrder.getStatus().toString());
        order.setPaymentType(becknOrder.getPayments().get(0)._getPaymentType());
        order.setFulfillmentType(becknOrder.getFulfillment().getType());
        order.setPaymentStatus(becknOrder.getPayments().get(0).getStatus().toString());
        order.setInvoiceAmount(becknOrder.getPayments().get(0).getParams().getAmount());
        order.setArchived(m.isArchived());
        order.setProviderId(becknOrder.getProvider().getId());
        Agent agent = becknOrder.getFulfillment().getAgent();
        if (agent != null) {
            Contact contact = agent.getContact();
            if (contact != null) {
                order.setDeliveryPartnerPhoneNumber(Phone.sanitizePhoneNumber(contact.getPhone()));
            }
        }
        
        order.save();
    }
    public void createPaymentLink() {
        BecknOrderMeta message = getProxy();
        
        Request tmp = new Request(){{
            setContext(new Context(message.getContextJson()){{
                setMessageId(UUID.randomUUID().toString());
                setTimestamp(new Date());
            }});
            setMessage(new Message(){{
                setOrder(new Order(message.getOrderJson()));
            }});
        }};
        Request request = new Request();
        request.setObjectCreator(NetworkManager.getInstance().getNetworkAdaptor().getObjectCreator(tmp.getContext().getDomain()));
        request.update(tmp);
        
        String providerId = request.getMessage().getOrder().getProvider().getId();
        Subscriber self = NetworkManager.getInstance().getSubscriber(Subscriber.SUBSCRIBER_TYPE_BPP);
        
        
        Call<JSONObject> call = new Call<JSONObject>().url(NetworkManager.getInstance().getNetworkAdaptor().getBaseUrl()+"/payment/createLink/%s".formatted(providerId)).
                header("Authorization",request.generateAuthorizationHeader(self.getSubscriberId(),self.getPubKeyId())).
                header("Content-Type", MimeType.APPLICATION_JSON.toString()).
                input(request.getInner()).inputFormat(InputFormat.JSON);
        
        JSONObject object = call.getResponseAsJson();
        
        tmp = new Request(object);
        tmp.setObjectCreator(request.getObjectCreator());
        request.getMessage().getOrder().setInvoices(tmp.getMessage().getOrder().getInvoices());
        Order order = request.getMessage().getOrder();
        for (Invoice invoice : order.getInvoices()){
            if (!invoice.isEstimate() && invoice.getPaymentTransactions().isEmpty()){
                //Unpaid invoice
                Payment term = null;
                for (Payment payment : order.getPayments()){
                    if (payment.getFulfillmentId() == null || ObjectUtil.equals(payment.getFulfillmentId(),invoice.getFulfillmentId())){
                        if (PaymentStatus.valueOf(payment.getStatus().literal()) == PaymentStatus.NOT_PAID) {
                            term = payment;
                            term.setFulfillmentId(invoice.getFulfillmentId());
                            term.getParams().setAmount(invoice.getAmount());
                            term.setUri(invoice.getTag("payment_link","uri"));
                            break;
                        }else {
                            throw new SellerException.PaymentNotSupported("Cannot create payment link when payment is already initiated.");
                        }
                    }
                }
                //Fix the first unpaid payment on payment object,
                break;
            }
        }
        
        message.setOrderJson(order.getInner().toString());
        message.save();
        
    }
    public void updatePayment(PaymentStatusEvent event){
        BecknOrderMeta message = getProxy();

        Order order = new Order(message.getOrderJson());
        
        for (Invoice invoice : order.getInvoices()) {
            String paymentUri = invoice.getTag("payment_link","uri");
            
            if (invoice.getPaymentTransactions().isEmpty() && ObjectUtil.equals(paymentUri,event.getUri())){
                if (event.getAmountPaid() >0 ) {
                    invoice.setPaymentTransactions(new PaymentTransactions() {{
                        add(new PaymentTransaction() {{
                            this.setAmount(event.getAmountPaid());// wE don't allow partial payment.
                            this.setPaymentStatus(PaymentStatus.convertor.valueOf(event.getStatus()));
                            this.setPaymentMethod(PaymentMethod.ONLINE_TRANSFER);
                            this.setTransactionId(event.getTxnReference());
                            this.setRemarks("Payment for Order %s".formatted(order.getId()));
                            this.setDate(new Date());
                        }});
                    }});
                }
                for (Payment payment : order.getPayments()){
                    if (ObjectUtil.equals(payment.getUri(),paymentUri)){
                        payment.setStatus(PaymentStatus.convertor.valueOf(event.getStatus()));
                        payment.getParams().setAmount(event.getAmountPaid());
                    }
                }
            }
        }
        message.setOrderJson(order.getInner().toString());
        message.save();
    }
    
    //Payload is always in network format. We store in raw beckn core format.
    public String getPayLoad(){
        BecknOrderMeta meta = getProxy();
        Request tmp =  new Request(){{
            setContext(new Context(meta.getContextJson()));
            setMessage(new Message(){{
                setOrder(new Order(meta.getOrderJson()));
            }});
        }};
        Request request = new Request();
        request.setObjectCreator(NetworkManager.getInstance().getNetworkAdaptor().getObjectCreator(tmp.getContext().getDomain()));
        request.update(tmp);
        return request.getInner().toString();
    }
    public void setPayLoad(String payload){
        Request request= new Request(payload);
        request.setObjectCreator(NetworkManager.getInstance().getNetworkAdaptor().getObjectCreator(request.getContext().getDomain()));
        
        Request tmp = new Request();
        tmp.update(request);
        BecknOrderMeta meta = getProxy();
        meta.setContextJson(tmp.getContext().getInner().toString());
        meta.setOrderJson(tmp.getMessage().getOrder().getInner().toString());
    }
    
}
