package in.succinct.bpp.core.db.model.igm.config;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;
import in.succinct.bpp.core.db.model.Subscriber;

public interface PreferredOdr extends Model {
    @PARTICIPANT
    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();


    public Long getOdrId();
    public void setOdrId(Long id);
    public Odr getOdr();

}
