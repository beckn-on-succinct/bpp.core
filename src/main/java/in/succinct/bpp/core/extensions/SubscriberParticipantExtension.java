package in.succinct.bpp.core.extensions;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.pm.DataSecurityFilter;
import in.succinct.bpp.core.db.model.Subscriber;
import in.succinct.bpp.core.db.model.igm.config.Application;

import java.util.Arrays;
import java.util.List;

public class SubscriberParticipantExtension extends ParticipantExtension<Subscriber> {
    static {
        registerExtension(new SubscriberParticipantExtension());
    }
    @Override
    public List<Long> getAllowedFieldValues(User user, Subscriber partiallyFilledModel, String fieldName) {
        List<Long> ret = null;
        if ("ANY_USER_ID".equalsIgnoreCase(fieldName)){
            ret = Arrays.asList(user.getId());
        }else if ("APPLICATION_ID".equalsIgnoreCase(fieldName)){
            ret = DataSecurityFilter.getIds(DataSecurityFilter.getRecordsAccessible(Application.class,user));
        }
        return ret;
    }
}
