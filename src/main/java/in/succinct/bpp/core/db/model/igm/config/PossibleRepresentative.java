package in.succinct.bpp.core.db.model.igm.config;

import com.venky.swf.db.model.User;
import in.succinct.bpp.core.db.model.Subscriber;

public interface PossibleRepresentative {

    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();


    public Long getUserId();
    public void setUserId(Long id);
    public User getUser();


}
