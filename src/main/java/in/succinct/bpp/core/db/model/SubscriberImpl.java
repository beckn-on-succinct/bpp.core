package in.succinct.bpp.core.db.model;

import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.plugins.collab.db.model.user.User;

public class SubscriberImpl extends ModelImpl<Subscriber> {
    public SubscriberImpl() {
    }

    public SubscriberImpl(Subscriber proxy) {
        super(proxy);
    }

    public String getName(){
        return String.format("%s-%s" , getProxy().getNetworkId(), getProxy().getSubscriberId());
    }

    public Long getAnyUserId(){
        return null;
    }
    public void setAnyUserId(Long anyUserId) {

    }
    public User getAnyUser(){
        return null;
    }

}
