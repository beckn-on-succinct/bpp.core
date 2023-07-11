package in.succinct.bpp.core.db.model.igm;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.user.User;
import in.succinct.bpp.core.db.model.Subscriber;
import in.succinct.bpp.core.db.model.igm.config.Channel;


public interface Representative extends Model {
    @UNIQUE_KEY
    @IS_NULLABLE(false)
    public long getIssueId();
    public void setIssueId(long id);
    public Issue getIssue();

    @UNIQUE_KEY
    @Enumeration(enumClass = "in.succinct.beckn.Role")
    //COMPLAINANT_PARTY,COMPLAINANT_PLATFORM, COMPLAINANT_GRO
    public String getRole();
    public void setRole(String role);

    @IS_NULLABLE(value = false)
    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();


    public Long getChannelId();
    public void setChannelId(Long id);
    public Channel getChannel();


    public Long getUserId();
    public void setUserId(Long id);
    public User getUser();
    // User Role would be "TP", "IRO", "GRO" , "ODR"

}
