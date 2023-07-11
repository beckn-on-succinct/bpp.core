package in.succinct.bpp.core.db.model.igm.config;

import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.user.User;
import in.succinct.bpp.core.db.model.Subscriber;

public interface PossibleRepresentative extends Model {

    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();


    public Long getUserId();
    public void setUserId(Long id);
    public User getUser();


}
