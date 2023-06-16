package in.succinct.bpp.core.db.model;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import in.succinct.bpp.core.db.model.igm.config.Application;
import in.succinct.bpp.core.db.model.igm.config.Channel;
import in.succinct.bpp.core.db.model.igm.config.Faq;
import in.succinct.bpp.core.db.model.igm.config.PossibleRepresentative;
import in.succinct.bpp.core.db.model.igm.config.PreferredOdr;
import in.succinct.bpp.core.db.model.rsp.BankAccount;

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

    @Enumeration(value = in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BAP + "," + in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BPP)
    public String getRole();
    public void setRole(String role);


    @UNIQUE_KEY("SID")
    public String getSubscriberId();
    public void setSubscriberId(String subscriberId);


    @UNIQUE_KEY("RSP_SID")
    public String getRspSubscriberId();
    public void setRspSubscriberId(String rspSubscriberId);


    /* Additional sources.*/
    /* Resolution source */
    public List<Channel> getChannels();

    public List<PossibleRepresentative>  getPossibleRepresentatives();

    public List<Faq> getFaqs();

    public List<PreferredOdr> getPreferredOdrs();

    public Long getBankAccountId();
    public void setBankAccountId(Long id);
    public BankAccount getBankAccount();


}
