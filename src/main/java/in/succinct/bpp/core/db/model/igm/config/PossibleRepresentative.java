package in.succinct.bpp.core.db.model.igm.config;

import com.venky.swf.db.model.User;

public interface PossibleRepresentative {

    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();


    public Long getUserId();
    public void setUserId(Long id);
    public User getUser();


}
