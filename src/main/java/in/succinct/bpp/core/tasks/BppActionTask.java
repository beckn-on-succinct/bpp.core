package in.succinct.bpp.core.tasks;

import com.venky.swf.plugins.beckn.tasks.BecknApiCall;
import com.venky.swf.plugins.beckn.tasks.BppTask;
import in.succinct.beckn.Request;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.api.NetworkApiAdaptor;

import java.util.Map;


public class BppActionTask extends BppTask {
    NetworkApiAdaptor networkApiAdaptor;
    CommerceAdaptor adaptor ;



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
        return callbackRequest;
    }

    @Override
    protected BecknApiCall send(Request callbackRequest) {
        String domain = callbackRequest.getContext().getDomain();
        return send(callbackRequest,networkApiAdaptor.getNetworkAdaptor().getDomains().get(domain).getSchema());
    }

    protected BecknApiCall send(Request callbackRequest,String schemaSource){
        BecknApiCall apiCall = super.send(callbackRequest.getContext().getBapUri() , callbackRequest,schemaSource);
        networkApiAdaptor.log("ToNetwork",callbackRequest,apiCall.getHeaders(),apiCall.getResponse(),apiCall.getUrl());
        return apiCall;
    }

    @Override
    protected void sendError(Throwable th) {
        String domain = getRequest().getContext().getDomain();
        super.sendError(th,networkApiAdaptor.getNetworkAdaptor().getDomains().get(domain).getSchema());
    }


}
