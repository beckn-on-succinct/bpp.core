package in.succinct.bpp.core.tasks;

import com.venky.swf.plugins.beckn.tasks.BecknApiCall;
import com.venky.swf.plugins.beckn.tasks.BppTask;
import in.succinct.beckn.Request;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.api.NetworkApiAdaptor;
import in.succinct.onet.core.api.MessageLogger;

import java.net.URL;
import java.util.Map;


public class BppActionTask extends BppTask {
    NetworkApiAdaptor networkApiAdaptor;
    CommerceAdaptor adaptor ;

    @Override
    public Priority getTaskPriority() {
        return Priority.HIGH;
    }

    public BppActionTask(NetworkApiAdaptor apiAdaptor, CommerceAdaptor adaptor, Request request, Map<String, String> headers){
        super(request,headers);
        this.adaptor = adaptor;
        this.networkApiAdaptor = apiAdaptor;
        this.setSubscriber(adaptor.getSubscriber());
    }

    @Override
    public Request generateCallBackRequest() {

        Request request = getRequest();
        String action = getRequest().getContext().getAction();

        Request callbackRequest = new Request();
        networkApiAdaptor.call(adaptor,getHeaders(),request,callbackRequest);

        /*
        Request finalCallbackRequest = networkApiAdaptor.getNetworkAdaptor().getObjectCreator(adaptor.getSubscriber().getDomain()).create(Request.class);
        finalCallbackRequest.update(callbackRequest);
        return finalCallbackRequest;
        
         */
        return callbackRequest;
    }

    @Override
    protected BecknApiCall send(Request callbackRequest) {
        String domain = callbackRequest.getContext().getDomain();
        return send(callbackRequest,networkApiAdaptor.getNetworkAdaptor().getDomains().get(domain).getSchemaURL());
    }

    protected BecknApiCall send(Request callbackRequest, URL schemaSource){
        Request request = getRequest();
        String callBackUrl = request == null ? null : request.getExtendedAttributes().get(Request.CALLBACK_URL); // To support call back via bg

        BecknApiCall apiCall = super.send(callBackUrl, callbackRequest,schemaSource);
        networkApiAdaptor.log(MessageLogger.TO_NET,callbackRequest,apiCall.getHeaders(),apiCall.getResponse(),apiCall.getUrl());
        return apiCall;
    }

    @Override
    protected void sendError(Throwable th) {
        String domain = getRequest().getContext().getDomain();
        super.sendError(th,networkApiAdaptor.getNetworkAdaptor().getDomains().get(domain).getSchemaURL());
    }


}
