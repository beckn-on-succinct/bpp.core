package in.succinct.bpp.core.adaptor.api;

import com.venky.core.util.ExceptionUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.beckn.tasks.BecknApiCall;
import com.venky.swf.routing.Config;
import in.succinct.beckn.BecknAware;
import in.succinct.beckn.BecknException;
import in.succinct.beckn.Cancellation;
import in.succinct.beckn.Cancellation.CancelledBy;
import in.succinct.beckn.CancellationReasons.CancellationReasonCode;
import in.succinct.beckn.Context;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Option;
import in.succinct.beckn.Order;
import in.succinct.beckn.Request;
import in.succinct.beckn.SellerException;
import in.succinct.beckn.SellerException.GenericBusinessError;
import in.succinct.beckn.SellerException.InvalidOrder;
import in.succinct.beckn.Subscriber;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.NetworkAdaptor;
import in.succinct.bpp.core.db.model.LocalOrderSynchronizerFactory;
import in.succinct.bpp.core.tasks.BppActionTask;
import org.json.simple.JSONObject;

import java.lang.reflect.Method;
import java.net.URL;
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

    public void call_by_pass(CommerceAdaptor adaptor, Map<String,String> headers, Request request, Request response){
        try {
            request.setObjectCreator(getNetworkAdaptor().getObjectCreator(adaptor.getSubscriber().getDomain()));
            response.setObjectCreator(getNetworkAdaptor().getObjectCreator(adaptor.getSubscriber().getDomain()));

            createReplyContext(adaptor.getSubscriber(),request,response);
            request.getContext().setNetworkId(getNetworkAdaptor().getId());
            response.getContext().setNetworkId(getNetworkAdaptor().getId());

            Method method = getClass().getMethod(request.getContext().getAction(), CommerceAdaptor.class, Request.class, Request.class);
            method.invoke(this, adaptor,request, response);

            response.getContext().setBppId(adaptor.getSubscriber().getSubscriberId());
            response.getContext().setBppUri(adaptor.getSubscriber().getSubscriberUrl());

            log(MessageLogger.TO_APP,request,headers,response,"/" + request.getContext().getAction());

            /*
            if (!response.isSuppressed()) {
                log("FromNetwork->ToNetwork", request, headers, response, "/" + request.getContext().getAction());
            }*/

        }catch (BecknException ex){
            throw ex;
        }catch (Exception ex){
            BecknException e =  (BecknException) ExceptionUtil.getEmbeddedException(ex,BecknException.class);
            if (e == null) {
                e = new GenericBusinessError(ExceptionUtil.getRootCause(ex).getMessage());
            }
            Config.instance().getLogger(getClass().getName()).log(Level.WARNING,e.getMessage(),ex);
            throw e;
        }
    }
    public void call(CommerceAdaptor adaptor, Map<String,String> headers, Request request, Request response){
        try {
            request.setObjectCreator(getNetworkAdaptor().getObjectCreator(adaptor.getSubscriber().getDomain()));
            response.setObjectCreator(getNetworkAdaptor().getObjectCreator(adaptor.getSubscriber().getDomain()));


            Request internalRequest  = new Request();
            internalRequest.update(request);
            JSONObject h = new JSONObject();h.putAll(headers);
            internalRequest.getExtendedAttributes().set("headers",h);
            internalRequest.getContext().setNetworkId(getNetworkAdaptor().getId());

            Request internalResponse = new Request();
            createReplyContext(adaptor.getSubscriber(),internalRequest,internalResponse);

            LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(adaptor.getSubscriber()).sync(internalRequest,getNetworkAdaptor(),false);

            Method method = getClass().getMethod(internalRequest.getContext().getAction(), CommerceAdaptor.class, Request.class, Request.class);
            method.invoke(this, adaptor,internalRequest, internalResponse);

            internalResponse.getContext().setBppId(adaptor.getSubscriber().getSubscriberId());
            internalResponse.getContext().setBppUri(adaptor.getSubscriber().getSubscriberUrl());

            log(MessageLogger.TO_APP,internalRequest,headers,internalResponse,"/" + internalRequest.getContext().getAction());

            LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(adaptor.getSubscriber()).sync(internalResponse,getNetworkAdaptor(),true);

            response.update(internalResponse);

            /*
            if (!response.isSuppressed()) {
                log("FromNetwork->ToNetwork", request, headers, response, "/" + request.getContext().getAction());
            }
             */

        }catch (BecknException ex){
            throw ex;
        }catch (Exception ex){
            BecknException e =  (BecknException) ExceptionUtil.getEmbeddedException(ex,BecknException.class);
            if (e == null) {
                e = new GenericBusinessError(ExceptionUtil.getRootCause(ex).getMessage());
            }
            Config.instance().getLogger(getClass().getName()).log(Level.WARNING,e.getMessage(),ex);
            throw e;
        }
    }
    public void search(CommerceAdaptor adaptor,Request request, Request reply){
        adaptor.search(request,reply);
    }

    //Called from search adaptor installer to cache the complete catalog!
    public void _search(CommerceAdaptor adaptor, Request reply) {
        adaptor._search(reply);
    }
    protected final TypeConverter<Double> doubleTypeConverter = Database.getJdbcTypeHelper("").getTypeRef(double.class).getTypeConverter();
    protected final TypeConverter<Boolean> booleanTypeConverter  = Database.getJdbcTypeHelper("").getTypeRef(boolean.class).getTypeConverter();


    public  void select(CommerceAdaptor adaptor,Request request, Request reply){
        adaptor.select(request,reply);
    }


    public void init(CommerceAdaptor adaptor, Request request, Request reply) {
        init(adaptor,request,reply,false);
    }
    private void init(CommerceAdaptor adaptor, Request request, Request reply,boolean syncMeta) {
        adaptor.init(request,reply);
        if (syncMeta){
            LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(adaptor.getSubscriber()).sync(reply,getNetworkAdaptor(),false);
        }
    }

    public void confirm(CommerceAdaptor adaptor, Request request, Request reply) {
        Order order = request.getMessage().getOrder();
        if (order == null){
            throw new InvalidOrder();
        }

        init(adaptor,request,reply,true);

        Request initReply = getNetworkAdaptor().getObjectCreator(adaptor.getSubscriber().getDomain()).create(Request.class);
        initReply.update(reply);

        adaptor.confirm(initReply,reply);
    }


    public void track(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.track(request,reply);
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

        CancellationReasonCode code = request.getMessage().getCancellationReasonCode();
        if (code != null && code.isUsableByBuyerParty()){
            Cancellation cancellation = order.getCancellation();
            if (cancellation == null){
                cancellation = new Cancellation();
                order.setCancellation(cancellation);
            }
            cancellation.setSelectedReason(new Option());
            cancellation.setCancelledBy(CancelledBy.BUYER);
            cancellation.setTime(request.getContext().getTimestamp());
            cancellation.getSelectedReason().setDescriptor(new Descriptor());
            cancellation.getSelectedReason().getDescriptor().setCode(CancellationReasonCode.convertor.toString(code));

        }else {
            throw new SellerException.InvalidCancellationReason();
        }
        LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(adaptor.getSubscriber()).sync(request,getNetworkAdaptor(),false);

        adaptor.cancel(request,reply);

    }

    public void update(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.update(request,reply);
    }

    public void status(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.status(request,reply);
    }

    public void rating(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.rating(request,reply);
    }

    public void issue(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.issue(request,reply);
    }

    public void issue_status(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.issue_status(request,reply);
    }

    public void receiver_recon(CommerceAdaptor adaptor, Request request, Request reply){
        adaptor.receiver_recon(request,reply);
    }

    public void support(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.support(request,reply);
    }

    public void get_cancellation_reasons(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.get_cancellation_reasons(request,reply);
    }

    public void get_return_reasons(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.get_return_reasons(request,reply);
    }

    public void get_rating_categories(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.get_rating_categories(request,reply);
    }

    public void get_feedback_categories(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.get_feedback_categories(request,reply);
    }

    public void get_feedback_form(CommerceAdaptor adaptor, Request request, Request reply) {
        adaptor.get_feedback_form(request,reply);
    }

    public void createReplyContext(Subscriber subscriber, Request from, Request to) {
        Context newContext = new Context();
        newContext.update(from.getContext());
        String action = from.getContext().getAction();
        newContext.setAction(action.startsWith("get_") ? action.substring(4) : "on_" + action);
        newContext.setBppId(subscriber.getSubscriberId());
        newContext.setBppUri(subscriber.getSubscriberUrl());
        to.setContext(newContext);
    }
    public void callback(CommerceAdaptor adaptor,Request reply) {
        callback(adaptor,reply,false);
    }
    protected void callback(CommerceAdaptor adaptor,Request reply,boolean passthrough) {

        if (reply.getContext() == null){
            throw new RuntimeException("Create Context before sending callback");
        }

        LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(adaptor.getSubscriber()).sync(reply,getNetworkAdaptor(),true);

        reply.getContext().setBppId(adaptor.getSubscriber().getSubscriberId());
        reply.getContext().setBppUri(adaptor.getSubscriber().getSubscriberUrl());

        Request networkReply ;
        if (passthrough) {
            networkReply = reply;
        }else {
            networkReply = getNetworkAdaptor().getObjectCreator(adaptor.getSubscriber().getDomain()).create(Request.class);
            networkReply.update(reply);
        }

        Map<String,Object> attributes = Database.getInstance().getCurrentTransaction().getAttributes();
        Map<String,Object> context = Database.getInstance().getContext();
        TaskManager.instance().executeAsync(new BppActionTask(this,adaptor, networkReply, new HashMap<>()) {
            @Override
            public Request generateCallBackRequest() {
                Database.getInstance().getCurrentTransaction().setAttributes(attributes);
                if (context != null){
                    context.remove(_IPath.class.getName());
                    Database.getInstance().setContext(context);
                }
                registerSignatureHeaders("Authorization");
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
        MessageLoggerFactory.getInstance().log(direction,request,headers,response);

    }
}
