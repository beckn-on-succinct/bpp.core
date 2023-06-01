package in.succinct.bpp.core.db.model.igm.config;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

import java.util.List;

public interface Subscriber extends Model {
    @UNIQUE_KEY("APP")
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    @UNIQUE_KEY("APP")
    public String getNetworkId();
    public void setNetworkId(String networkId);

    @UNIQUE_KEY("APP")
    public String getDomainId();
    public void setDomainId(String domainId);


    @UNIQUE_KEY("SID")
    public String getSubscriberId();
    public void setSubscriberId(String subscriberId);

    /* Additional sources.*/
    /* Resolution source */
    public List<Channel> getChannels();

    public List<PossibleRepresentative>  getPossibleRepresentatives();

    public List<Faq> getFaqs();

    public List<PreferredOdr> getPreferredOdrs();

}
