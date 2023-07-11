package in.succinct.bpp.core.adaptor.rsp;

import in.succinct.beckn.Request;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;

public abstract class ReceiverReconProvider {
    CommerceAdaptor adaptor;

    protected ReceiverReconProvider( CommerceAdaptor adaptor) {
        this.adaptor = adaptor;
    }

    public CommerceAdaptor getAdaptor() {
        return adaptor;
    }

    public abstract void receiver_recon(Request request,Request response);

    public Request createNetworkResponse(Request response){
        return response;
    }
}
