package in.succinct.bpp.core.db.model.igm.config;

import com.venky.swf.db.model.Model;

public interface PreferredOdr extends Model {
    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();


    public Long getOdrId();
    public void setOdrId(Long id);
    public Odr getOdr();

}
