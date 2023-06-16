package in.succinct.bpp.core.adaptor.rating;

import in.succinct.beckn.Request;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;

public abstract class RatingCollector {
    CommerceAdaptor adaptor;

    protected RatingCollector( CommerceAdaptor adaptor) {
        this.adaptor = adaptor;
    }

    public CommerceAdaptor getAdaptor() {
        return adaptor;
    }

    public abstract void rating(Request request, Request response); //Create or update issue

}
