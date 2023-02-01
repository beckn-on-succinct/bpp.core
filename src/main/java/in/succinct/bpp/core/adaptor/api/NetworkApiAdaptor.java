package in.succinct.bpp.core.adaptor.api;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.routing.Config;
import in.succinct.beckn.BecknAware;
import in.succinct.beckn.Context;
import in.succinct.beckn.Request;
import in.succinct.beckn.Subscriber;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.NetworkAdaptor;
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
    public abstract void _search(CommerceAdaptor adaptor,Request reply);

    public abstract void select(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void init(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void confirm(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void track(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void cancel(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void update(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void status(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void rating(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void support(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void get_cancellation_reasons(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void get_return_reasons(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void get_rating_categories(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void get_feedback_categories(CommerceAdaptor adaptor,Request request, Request reply);

    public abstract void get_feedback_form(CommerceAdaptor adaptor,Request request, Request reply);

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
}
