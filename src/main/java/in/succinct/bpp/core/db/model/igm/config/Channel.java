package in.succinct.bpp.core.db.model.igm.config;

import com.venky.swf.db.model.Model;
import in.succinct.bpp.core.db.model.Subscriber;

// Resolution source
public interface Channel extends Model {
    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();


    public String getName();
    public void setName(String name);

    /* this is an application associated with the subscriber's organizatriojn
    *  e.g chat app, ivr, support url,
    *
    *
    */
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

}
