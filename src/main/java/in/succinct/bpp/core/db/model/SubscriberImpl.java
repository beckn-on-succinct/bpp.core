package in.succinct.bpp.core.db.model;

import com.venky.swf.db.table.ModelImpl;

public class SubscriberImpl extends ModelImpl<Subscriber> {
    public SubscriberImpl() {
    }

    public SubscriberImpl(Subscriber proxy) {
        super(proxy);
    }

    public String getName(){
        return String.format("%s-%s" , getProxy().getNetworkId(), getProxy().getSubscriberId());
    }
}
