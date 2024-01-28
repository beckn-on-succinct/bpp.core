package in.succinct.bpp.core.adaptor.igm;

import in.succinct.beckn.Request;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;

public abstract class IssueTracker {
    CommerceAdaptor adaptor;

    protected IssueTracker(CommerceAdaptor adaptor) {
        this.adaptor = adaptor;
    }


    public CommerceAdaptor getAdaptor() {
        return adaptor;
    }
    public abstract Request createNetworkResponse(Request response);

    public abstract void save(Request request, Request response); //Create or update issue

    public abstract void status(Request request, Request response);
}
