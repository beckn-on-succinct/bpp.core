package in.succinct.bpp.core.adaptor.api;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.routing.Config;
import in.succinct.beckn.BecknAware;
import in.succinct.beckn.CancellationReasons;
import in.succinct.beckn.Catalog;
import in.succinct.beckn.Context;
import in.succinct.beckn.FeedbackCategories;
import in.succinct.beckn.Message;
import in.succinct.beckn.Order;
import in.succinct.beckn.Provider;
import in.succinct.beckn.Providers;
import in.succinct.beckn.RatingCategories;
import in.succinct.beckn.Request;
import in.succinct.beckn.ReturnReasons;
import in.succinct.beckn.Subscriber;
import in.succinct.beckn.Tracking;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.NetworkAdaptor;
import in.succinct.bpp.core.tasks.BppActionTask;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public abstract class NetworkApiAdaptor {
    private final NetworkAdaptor networkAdaptor ;
    public NetworkApiAdaptor(NetworkAdaptor networkAdaptor) {
        this.networkAdaptor = networkAdaptor;
    }

    public NetworkAdaptor getNetworkAdaptor() {
        return networkAdaptor;
    }

    public void call(CommerceAdaptor adaptor, Map<String,String> headers, Request request, Request response){
        JSONObject h = new JSONObject();h.putAll(headers);
        request.getExtendedAttributes().set("headers",h);
        try {
            createReplyContext(adaptor.getSubscriber(),request,response);
            Method method = getClass().getMethod(request.getContext().getAction(), CommerceAdaptor.class, Request.class, Request.class);
            method.invoke(this, adaptor,request, response);
            log("ToApplication",request,headers,response,"/" + request.getContext().getAction());
            response.getContext().setBppId(adaptor.getSubscriber().getSubscriberId());
            response.getContext().setBppUri(adaptor.getSubscriber().getSubscriberUrl());
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
    public abstract void search(CommerceAdaptor adaptor,Request request, Request reply);

    public void _search(CommerceAdaptor adaptor, Request reply) {
        Message message = new Message();
        reply.setMessage(message);

        Catalog catalog = new Catalog();
        message.setCatalog(catalog);

        Providers providers = new Providers();
        catalog.setProviders(providers);
        Provider provider = getNetworkAdaptor().create(Provider.class,adaptor.getSubscriber().getDomain());
        provider.update(adaptor.getProvider());
        providers.add(provider);
    }
    protected final TypeConverter<Double> doubleTypeConverter = Database.getJdbcTypeHelper("").getTypeRef(double.class).getTypeConverter();
    protected final TypeConverter<Boolean> booleanTypeConverter  = Database.getJdbcTypeHelper("").getTypeRef(boolean.class).getTypeConverter();


    public abstract void select(CommerceAdaptor adaptor,Request request, Request reply);


    public void init(CommerceAdaptor adaptor, Request request, Request reply) {
        Order order = getInputOrder(adaptor,request.getMessage().getOrder());

        Message message = new Message();
        reply.setMessage(message);

        Order draftOrder = adaptor.initializeDraftOrder(request);
        Order outOrder = getOutputOrder(adaptor,draftOrder);
        message.setOrder(outOrder);
    }

    public void confirm(CommerceAdaptor adaptor, Request request, Request reply) {
        Order order = request.getMessage().getOrder();
        if (order == null){
            throw new RuntimeException("No Order passed");
        }
        Order bapOrder = getInputOrder(adaptor,order);

        Message message = new Message(); reply.setMessage(message);
        Order draftOrder = adaptor.initializeDraftOrder(request); // RECompute

        Order confirmedOrder = adaptor.confirmDraftOrder(draftOrder);
        Order outOrder = getOutputOrder(adaptor,confirmedOrder);
        message.setOrder(outOrder);
    }


    public void track(CommerceAdaptor adaptor, Request request, Request reply) {
        /* Take track message and fill response with on_track message */
        Order order = request.getMessage().getOrder();
        if (order == null){
            throw new RuntimeException("No Order passed");
        }
        String trackUrl  = adaptor.getTrackingUrl(order);
        if (trackUrl != null) {
            Message message = new Message();
            reply.setMessage(message);
            message.setTracking(getNetworkAdaptor().create(Tracking.class, adaptor.getSubscriber().getDomain()));
            message.getTracking().setUrl(trackUrl);
        }
    }




    public void cancel(CommerceAdaptor adaptor, Request request, Request reply) {
        Order order = getInputOrder(adaptor,request.getMessage().getOrder());

        Order cancelledOrder = adaptor.cancel(order);
        Message message = new Message(); reply.setMessage(message);

        Order outOrder = getOutputOrder(adaptor,cancelledOrder);
        message.setOrder(outOrder);
    }

    public void update(CommerceAdaptor adaptor, Request request, Request reply) {
        throw new RuntimeException("Orders cannot be updated. Please cancel and rebook your orders!");
    }

    public void status(CommerceAdaptor adaptor, Request request, Request reply) {
        Order order = request.getMessage().getOrder();
        if (order == null){
            order = new Order();
            order.setId(request.getMessage().get("order_id"));
            request.getMessage().setOrder(order);
        }
        order = getInputOrder(adaptor,order);

        reply.setMessage(new Message());

        Order current = adaptor.getStatus(order);
        reply.getMessage().setOrder(getOutputOrder(adaptor,current));
    }

    public void rating(CommerceAdaptor adaptor, Request request, Request reply) {

    }

    public void support(CommerceAdaptor adaptor, Request request, Request reply) {
        reply.setMessage(new Message());
        reply.getMessage().setEmail(adaptor.getProviderConfig().getSupportContact().getEmail());
    }

    public void get_cancellation_reasons(CommerceAdaptor adaptor, Request request, Request reply) {
        reply.setCancellationReasons(new CancellationReasons());
    }

    public void get_return_reasons(CommerceAdaptor adaptor, Request request, Request reply) {
        reply.setReturnReasons(new ReturnReasons());
    }

    public void get_rating_categories(CommerceAdaptor adaptor, Request request, Request reply) {
        reply.setRatingCategories(new RatingCategories());
    }

    public void get_feedback_categories(CommerceAdaptor adaptor, Request request, Request reply) {
        reply.setFeedbackCategories(new FeedbackCategories());
    }

    public void get_feedback_form(CommerceAdaptor adaptor, Request request, Request reply) {
    }

    public void createReplyContext(Subscriber subscriber, Request from, Request to) {
        Context newContext = ObjectUtil.clone(from.getContext());
        String action = from.getContext().getAction();
        newContext.setAction(action.startsWith("get_") ? action.substring(4) : "on_" + action);
        newContext.setBppId(subscriber.getSubscriberId());
        newContext.setBppUri(subscriber.getSubscriberUrl());
        to.setContext(newContext);
    }

    public void callback(CommerceAdaptor adaptor,Request reply) {

        if (reply.getContext() == null){
            throw new RuntimeException("Create Context before sending callback");
        }
        reply.getContext().setBppId(adaptor.getSubscriber().getSubscriberId());
        reply.getContext().setBppUri(adaptor.getSubscriber().getSubscriberUrl());
        TaskManager.instance().executeAsync(new BppActionTask(this,adaptor, reply, new HashMap<>()) {
            @Override
            public Request generateCallBackRequest() {
                registerSignatureHeaders("Authorization");
                log("ToApplication", reply, getHeaders(), reply, "/" + reply.getContext().getAction());
                return reply;
            }
        }, false);
    }

    public void log(String direction,
                    Request request, Map<String, String> headers, BecknAware response,
                    String url) {
        Map<String,String> maskedHeaders = new HashMap<>();
        headers.forEach((k,v)->{
            maskedHeaders.put(k, Config.instance().isDevelopmentEnvironment()? v : "***");
        });
        Config.instance().getLogger(BppActionTask.class.getName()).log(Level.INFO,String.format("%s|%s|%s|%s|%s",direction,request,headers,response,url));

    }
    protected Order getInputOrder(CommerceAdaptor adaptor, Order order){
        if (order == null){
            throw new RuntimeException("No Order passed");
        }
        Order networkOrder = getNetworkAdaptor().create(Order.class,adaptor.getSubscriber().getDomain());
        networkOrder.setInner((JSONObject) JSONValue.parse(order.getInner().toString()));

        Order becknOrder = new Order();
        becknOrder.update(networkOrder);

        order.setInner(becknOrder.getInner());
        return order;
    }

    protected Order getOutputOrder(CommerceAdaptor adaptor,Order order){
        if (order == null){
            throw new RuntimeException("No Order passed");
        }
        Order networkOrder = getNetworkAdaptor().create(Order.class,adaptor.getSubscriber().getDomain());
        networkOrder.update(order);


        return networkOrder;
    }
}
