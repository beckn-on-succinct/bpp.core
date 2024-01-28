package in.succinct.bpp.core.db.model;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.user.User;
import in.succinct.bpp.core.db.model.igm.config.Application;
import in.succinct.bpp.core.db.model.igm.config.Channel;
import in.succinct.bpp.core.db.model.igm.config.Faq;
import in.succinct.bpp.core.db.model.igm.config.PossibleRepresentative;
import in.succinct.bpp.core.db.model.igm.config.PreferredOdr;
import in.succinct.bpp.core.db.model.rsp.BankAccount;

import java.util.List;

public interface Subscriber extends Model {
    @IS_VIRTUAL
    @PARTICIPANT
    @HIDDEN
    public Long getAnyUserId();
    public void setAnyUserId(Long anyUserId);
    public User getAnyUser();


    @IS_VIRTUAL
    public String getName();

    @UNIQUE_KEY("APP")
    @PARTICIPANT
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    @UNIQUE_KEY("APP,SID,RSP_SID")
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
