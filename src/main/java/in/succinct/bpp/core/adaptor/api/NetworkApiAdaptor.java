package in.succinct.bpp.core.adaptor.api;

import com.venky.core.util.ExceptionUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.routing.Config;
import in.succinct.beckn.BecknAware;
import in.succinct.beckn.BecknException;
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
import in.succinct.beckn.SellerException.ActionNotApplicable;
import in.succinct.beckn.SellerException.GenericBusinessError;
import in.succinct.beckn.SellerException.InvalidOrder;
import in.succinct.beckn.SellerException.TrackingNotSupported;
import in.succinct.beckn.Subscriber;
import in.succinct.beckn.Tracking;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.NetworkAdaptor;
import in.succinct.bpp.core.db.model.LocalOrderSynchronizer;
import in.succinct.bpp.core.tasks.BppActionTask;
import org.json.simple.JSONObject;

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
        request.setObjectCreator(getNetworkAdaptor().getObjectCreator(adaptor.getSubscriber().getDomain()));
        try {
            createReplyContext(adaptor.getSubscriber(),request,response);

            LocalOrderSynchronizer.getInstance().sync(request,getNetworkAdaptor(),adaptor.getSubscriber(),false);

            Method method = getClass().getMethod(request.getContext().getAction(), CommerceAdaptor.class, Request.class, Request.class);
            method.invoke(this, adaptor,request, response);

            LocalOrderSynchronizer.getInstance().sync(response,getNetworkAdaptor(),adaptor.getSubscriber(),true);

            log("ToApplication",request,headers,response,"/" + request.getContext().getAction());
            response.getContext().setBppId(adaptor.getSubscriber().getSubscriberId());
            response.getContext().setBppUri(adaptor.getSubscriber().getSubscriberUrl());
        }catch (BecknException ex){
            throw ex;
        }catch (Exception ex){
            BecknException e = new GenericBusinessError(ExceptionUtil.getRootCause(ex).getMessage());
            Config.instance().getLogger(getClass().getName()).log(Level.WARNING,e.getMessage(),ex);
            throw e;
        }
    }
    public abstract void search(CommerceAdaptor adaptor,Request request, Request reply);

    //Called from serch adaptor to cache the complete catalog!
    public void _search(CommerceAdaptor adaptor, Request reply) {
        Message message = new Message();
        reply.setMessage(message);

        Catalog catalog = new Catalog();
        message.setCatalog(catalog);

        Providers providers = new Providers();
        catalog.setProviders(providers);
        Provider provider = adaptor.getProvider();
        providers.add(provider);
    }
    protected final TypeConverter<Double> doubleTypeConverter = Database.getJdbcTypeHelper("").getTypeRef(double.class).getTypeConverter();
    protected final TypeConverter<Boolean> booleanTypeConverter  = Database.getJdbcTypeHelper("").getTypeRef(boolean.class).getTypeConverter();


    public abstract void select(CommerceAdaptor adaptor,Request request, Request reply);


    public void init(CommerceAdaptor adaptor, Request request, Request reply) {

        Message message = new Message();
        reply.setMessage(message);

        Order draftOrder = adaptor.initializeDraftOrder(request);
        message.setOrder(draftOrder);
    }

    public void confirm(CommerceAdaptor adaptor, Request request, Request reply) {
        Order order = request.getMessage().getOrder();
        if (order == null){
            throw new InvalidOrder();
        }
        Message message = new Message(); reply.setMessage(message);
        Order draftOrder = adaptor.initializeDraftOrder(request); // RECompute
        message.setOrder(draftOrder); //Temporarily setit for synchronization purposes
        LocalOrderSynchronizer.getInstance().sync(reply,getNetworkAdaptor(),adaptor.getSubscriber(),false);

        Order confirmedOrder = adaptor.confirmDraftOrder(draftOrder);
        message.setOrder(confirmedOrder);
    }


    public void track(CommerceAdaptor adaptor, Request request, Request reply) {
        /* Take track message and fill response with on_track message */
        Order order = request.getMessage().getOrder();
        if (order == null){
            throw new InvalidOrder();
        }
        String trackUrl  = adaptor.getTrackingUrl(order);
        Message message = new Message();
        reply.setMessage(message);

        if (trackUrl != null) {
            message.setTracking(new Tracking());
            message.getTracking().setUrl(trackUrl);
        }else {
            throw new TrackingNotSupported();
        }
    }




    public void cancel(CommerceAdaptor adaptor, Request request, Request reply) {
        Order order = request.getMessage().getOrder();
        if (order == null){
            if (request.getMessage().getOrderId() != null){
                order = new Order();
                order.setId(request.getMessage().getOrderId());
            }
        }
        if (order == null){
            throw new InvalidOrder();
        }
        Order cancelledOrder = adaptor.cancel(order);
        Message message = new Message(); reply.setMessage(message);
        message.setOrder(cancelledOrder);
    }

    public void update(CommerceAdaptor adaptor, Request request, Request reply) {
        throw new ActionNotApplicable("Orders cannot be updated. Please cancel and rebook your orders!");
    }

    public void status(CommerceAdaptor adaptor, Request request, Request reply) {
        Order order = request.getMessage().getOrder();
        if (order == null){
            if (request.getMessage().getOrderId() != null){
                order = new Order();
                order.setId(request.getMessage().getOrderId());
                request.getMessage().setOrder(order);
            }
        }
        if (order == null){
            throw new InvalidOrder();
        }
        reply.setMessage(new Message());
        Order current = adaptor.getStatus(order);
        reply.getMessage().setOrder(current);
    }

    public void rating(CommerceAdaptor adaptor, Request request, Request reply) {

    }

    public void support(CommerceAdaptor adaptor, Request request, Request reply) {
        reply.setMessage(new Message());
        reply.getMessage().setEmail(adaptor.getProviderConfig().getSupportContact().getEmail());
        reply.getMessage().setPhone(adaptor.getProviderConfig().getSupportContact().getPhone());
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

        LocalOrderSynchronizer.getInstance().sync(reply,getNetworkAdaptor(),adaptor.getSubscriber(),true);

        reply.getContext().setBppId(adaptor.getSubscriber().getSubscriberId());
        reply.getContext().setBppUri(adaptor.getSubscriber().getSubscriberUrl());


        Request networkReply = getNetworkAdaptor().getObjectCreator(adaptor.getSubscriber().getDomain()).create(Request.class);
        networkReply.update(reply);
        Map<String,Object> attributes = Database.getInstance().getCurrentTransaction().getAttributes();
        TaskManager.instance().executeAsync(new BppActionTask(this,adaptor, networkReply, new HashMap<>()) {
            @Override
            public Request generateCallBackRequest() {
                Database.getInstance().getCurrentTransaction().setAttributes(attributes);
                registerSignatureHeaders("Authorization");
                log("ToApplication", networkReply, getHeaders(), networkReply, "/" + networkReply.getContext().getAction());
                return networkReply;
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
}
