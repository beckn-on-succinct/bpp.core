package in.succinct.bpp.core.adaptor;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import com.venky.swf.routing.Config;
import in.succinct.beckn.BecknAware;
import in.succinct.beckn.Context;
import in.succinct.beckn.Request;
import in.succinct.bpp.core.registry.BecknRegistry;
import in.succinct.bpp.core.tasks.BppActionTask;
import org.json.simple.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public abstract class CommerceAdaptor {
    private final Subscriber subscriber;
    private final BecknRegistry registry;

    public CommerceAdaptor(Subscriber subscriber, BecknRegistry registry) {
        this.subscriber = subscriber;
        this.registry = registry;
    }

    public BecknRegistry getRegistry() {
        return registry;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void call(Map<String,String> headers, Request request, Request response){
        JSONObject h = new JSONObject();h.putAll(headers);
        request.getExtendedAttributes().set("headers",h);
        try {
            createReplyContext(request,response);
            Method method = getClass().getMethod(request.getContext().getAction(), Request.class, Request.class);
            method.invoke(this, request, response);
            log("ToApplication",request,headers,response,"/" + request.getContext().getAction());
            response.getContext().setBppId(getSubscriber().getSubscriberId());
            response.getContext().setBppUri(getSubscriber().getSubscriberUrl());
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }

    }
    public abstract void search(Request request, Request reply);

    public abstract void select(Request request, Request reply);

    public abstract void init(Request request, Request reply);

    public abstract void confirm(Request request, Request reply);

    public abstract void track(Request request, Request reply);

    public abstract void cancel(Request request, Request reply);

    public abstract void update(Request request, Request reply);

    public abstract void status(Request request, Request reply);

    public abstract void rating(Request request, Request reply);

    public abstract void support(Request request, Request reply);

    public abstract void get_cancellation_reasons(Request request, Request reply);

    public abstract void get_return_reasons(Request request, Request reply);

    public abstract void get_rating_categories(Request request, Request reply);

    public abstract void get_feedback_categories(Request request, Request reply);

    public abstract void get_feedback_form(Request request, Request reply);

    public void createReplyContext(Request from, Request to) {
        Context newContext = ObjectUtil.clone(from.getContext());
        String action = from.getContext().getAction();
        newContext.setAction(action.startsWith("get_") ? action.substring(4) : "on_" + action);
        newContext.setBppId(getSubscriber().getSubscriberId());
        newContext.setBppUri(getSubscriber().getSubscriberUrl());
        to.setContext(newContext);
    }

    public void callback(Request reply) {

        if (reply.getContext() == null){
            throw new RuntimeException("Create Context before sending callback");
        }
        reply.getContext().setBppId(getSubscriber().getSubscriberId());
        reply.getContext().setBppUri(getSubscriber().getSubscriberUrl());
        TaskManager.instance().executeAsync(new BppActionTask(this, reply, new HashMap<>()) {
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
