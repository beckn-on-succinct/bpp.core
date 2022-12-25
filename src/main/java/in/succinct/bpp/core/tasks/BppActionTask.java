package in.succinct.bpp.core.tasks;

import com.venky.swf.plugins.beckn.tasks.BecknApiCall;
import com.venky.swf.plugins.beckn.tasks.BppTask;
import in.succinct.beckn.Request;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.registry.BecknRegistry;

import java.util.Map;


public class BppActionTask extends BppTask {
    CommerceAdaptor adaptor ;

    public BecknRegistry getRegistry() {
        return adaptor.getRegistry();
    }

    public BppActionTask(CommerceAdaptor adaptor, Request request, Map<String, String> headers){
        super(request,headers);
        this.adaptor = adaptor;
        this.setSubscriber(adaptor.getSubscriber());
    }

    @Override
    public Request generateCallBackRequest() {

        Request request = getRequest();
        String action = getRequest().getContext().getAction();

        Request callbackRequest = new Request();
        adaptor.call(getHeaders(),request,callbackRequest);
        return callbackRequest;
    }

    @Override
    protected BecknApiCall send(Request callbackRequest) {
        return send(callbackRequest,getRegistry().getSchema());
    }

    protected BecknApiCall send(Request callbackRequest,String schemaSource){
        BecknApiCall apiCall = super.send(callbackRequest.getContext().getBapUri() , callbackRequest,schemaSource);
        adaptor.log("ToNetwork",callbackRequest,apiCall.getHeaders(),apiCall.getResponse(),apiCall.getUrl());
        return apiCall;
    }

    @Override
    protected void sendError(Throwable th) {
        super.sendError(th,getRegistry().getSchema());
    }


}
