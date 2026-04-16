package in.succinct.bpp.core.controller;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.path.Path;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.View;
import in.succinct.beckn.Agent;
import in.succinct.beckn.Context;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Fulfillment;
import in.succinct.beckn.Fulfillment.FulfillmentStatus;
import in.succinct.beckn.Order;
import in.succinct.beckn.Organization;
import in.succinct.beckn.Request;
import in.succinct.beckn.SellerException;
import in.succinct.bpp.core.db.model.Message;
import in.succinct.bpp.core.db.model.User;
import in.succinct.bpp.core.util.NetworkManager;
import in.succinct.events.PaymentStatusEvent;
import in.succinct.onet.core.adaptor.NetworkAdaptor.Domain;
import in.succinct.onet.core.adaptor.NetworkAdaptor.DomainCategory;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class MessagesController extends ModelController<Message> {
    public MessagesController(Path path) {
        super(path);
    }

    private View list(boolean archived){
        Expression where = getWhereClause();
        where.add(new Expression(getReflector().getPool(),"ARCHIVED",Operator.EQ,archived));
        
        Select q = new Select().from(getModelClass());
        List<Message> records = q.where(where).orderBy(getReflector().getOrderBy()).execute(getModelClass(), Select.MAX_RECORDS_ALL_RECORDS, getFilter());
        return list(records,true);
    }
    
    public View live(){
        return list(false);
    }
    public View history(){
        return list(true);
    }
    
    @Override
    protected void rewriteQuery(Map<String, Object> formData) {
        User user = getPath().getSessionUser().getRawRecord().getAsProxy(User.class);
        if  (user.getId() > 1 && formData.containsKey("q")) {
            StringBuilder finalFilter = new StringBuilder("(").append(formData.get("q")).append(")");
            
            StringBuilder addnlfilter = new StringBuilder();
            if (!ObjectUtil.isVoid(user.getProviderId())) {
                addnlfilter.append("PROVIDER_ID:%s".formatted(user.getProviderId()));
            }
            if (!addnlfilter.isEmpty()){
                finalFilter.append(" AND (").append(addnlfilter).append(")");
            }
            formData.put("q",finalFilter.toString());
        }
        super.rewriteQuery(formData);
    }
    protected Expression getWhereClause(){
        
        Expression where = super.getWhereClause();
        User user = getPath().getSessionUser().getRawRecord().getAsProxy(User.class);
        if  (user.getId() > 1) {
            Expression addl = new Expression(getReflector().getPool(), Conjunction.AND);
            addl.add(new Expression(getReflector().getPool(),"PROVIDER_ID" ,Operator.EQ, user.getProviderId()));
            addl.add(new Expression(getReflector().getPool(),"SUBSCRIBER_ID" ,Operator.EQ, NetworkManager.getInstance().getSubscriberId()));
            where.add(addl);
        }
        
        return where;
    }
    
    
    @SingleRecordAction(icon = "fa-link")
    public View createPaymentLink(long id){
        Message message = Database.getTable(getModelClass()).get(id);
        message.createPaymentLink();
        return show(id);
    }
    
    /**
     * new JSONObject() {{
     *                 put("txn_reference", link.getTxnReference());
     *                 put("status", link.getStatus());
     *                 put("active",link.isActive());
     *                 put("uri", link.getLinkUri());
     *             }}
     * @return
     */
    
    @RequireLogin(false)
    public View updatePayment(){
        try {
            String payload = StringUtil.read(getPath().getInputStream());
            Request request = new Request(payload);
            if (!request.verifySignature("Authorization",getPath().getHeaders())){
                throw new SellerException.InvalidSignature();
            }
            
            PaymentStatusEvent event = new PaymentStatusEvent(payload);
            
            Message message = Database.getTable(Message.class).newRecord();
            message.setBecknTransactionId(event.getTransactionId());
            message.setSubscriberId(NetworkManager.getInstance().getSubscriberId());
            
            message = Database.getTable(Message.class).getRefreshed(message);
            if (message.getRawRecord().isNewRecord()){
                throw new RuntimeException("Cannot identify beckn transaction_id");
            }
            message.updatePayment(event);
            return no_content();
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
        
        
    }
    
    public View refresh(long id){
        Message m  = Database.getTable(getModelClass()).get(id);
        if (m == null || !m.isAccessibleBy(getSessionUser())) {
            throw new AccessDeniedException();
        }
        Order order = new Order(m.getOrderJson());
        Context context = new Context(m.getContextJson());
        
        String deliverySubscriber = getDeliverySubscriber(order);
        Fulfillment fulfillment = order.getFulfillment();
        
        if (!ObjectUtil.isVoid(deliverySubscriber) && fulfillment != null){
            String txnId = fulfillment.getTag("delivery_order","transaction_id");
            String orderId = fulfillment.getTag("delivery_order","order_id");
            if (!ObjectUtil.isVoid(orderId)){
                Request deliveryStatus = createStatusResponse(txnId,orderId,deliverySubscriber);
                if (deliveryStatus != null){
                    Order deliveryOrder = deliveryStatus.getMessage().getOrder();
                    switch (deliveryOrder.getStatus()){
                        case Completed -> {
                            fulfillment.setFulfillmentStatus(FulfillmentStatus.Completed);
                        }
                        case In_Transit -> {
                            fulfillment.setFulfillmentStatus(FulfillmentStatus.In_Transit);
                        }
                    }
                }
            }
            m.setOrderJson(order.getInner().toString());
            m.save();
        }
        
        return show(m);
    }
    
    
    
    private Request createStatusResponse(String txnId, String orderId, String deliverySubscriber) {
        Domain logistics = null;
        for (Domain domain : NetworkManager.getInstance().getNetworkAdaptor().getDomains()) {
            if (domain.getDomainCategory() == DomainCategory.HIRE_TRANSPORT_SERVICE){
                logistics = domain;
                break;
            }
        }
        Domain request_domain = logistics;
        Request request = new Request(){{
           setContext(new Context(){{
               setBppId(deliverySubscriber);
               setTransactionId(txnId);
               setAction("status");
               setNetworkId(NetworkManager.getInstance().getNetworkAdaptor().getId());
               setDomain(request_domain == null ? null : request_domain.getId());
           }});
           setMessage(new in.succinct.beckn.Message(){{
               setOrder(new Order(){{
                   setId(orderId);
               }});
           }});
        }};
        Request networkRequest = NetworkManager.getInstance().getNetworkAdaptor().getObjectCreator(request.getContext().getDomain()).create(Request.class);
        networkRequest.update(request);
        
        String bg = NetworkManager.getInstance().getNetworkAdaptor().getSearchProvider().getSubscriberUrl();
        JSONAware response = new Call<JSONObject>().url(bg,"status").inputFormat(InputFormat.JSON).input(networkRequest.getInner()).
                header("Content-type","application/json").header("X-CallBackToBeSynchronized","Y").header("ApiKey",getSessionUser().getApiKey()).
                getResponseAsJson();
        if (response != null) {
            if (response instanceof JSONArray responses){
                response = responses.size() != 1 ? null : (JSONObject)responses.get(0);
            }
        }
        if (response != null){
            Request deliveryResponse = networkRequest.getObjectCreator().create(Request.class);
            deliveryResponse.setInner((JSONObject) response);
            return  deliveryResponse;
        }
        
        return null;
    }
    
    private String getDeliverySubscriber(Order order) {
        Fulfillment fulfillment  = order.getFulfillment();
        Agent agent  = fulfillment == null ? null : fulfillment.getAgent();
        Organization organization = agent == null ? null : agent.getOrganization();
        Descriptor descriptor = organization == null ? null : organization.getDescriptor();
        return descriptor == null ? null : descriptor.getCode();
    }
    
    @SingleRecordAction(icon = "fa-solid fa-table-list")
    public View summarize(long id){
        Database.getTable(getModelClass()).get(id).summarize(true);
        if (getIntegrationAdaptor() == null) {
            return back();
        }else {
            return getIntegrationAdaptor().createStatusResponse(getPath(),null);
        }
    }
}
