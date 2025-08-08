package in.succinct.bpp.core.extensions;

import com.venky.core.collections.SequenceSet;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import in.succinct.bpp.core.db.model.AdaptorCredential;

import java.util.List;

public class AdaptorCredentialParticipantExtension extends ParticipantExtension<AdaptorCredential> {
    static {
        registerExtension(new AdaptorCredentialParticipantExtension());
    }
    
    @Override
    protected List<Long> getAllowedFieldValues(User user, AdaptorCredential partiallyFilledModel, String fieldName) {
        List<Long> ret = null;
        if (ObjectUtil.equals(fieldName,"USER_ID")){
            if (!user.getRawRecord().getAsProxy(in.succinct.bpp.core.db.model.User.class).isStaff()){
                ret = new SequenceSet<>();
                ret.add(user.getId());
            }
        }
        return ret;
    }
}
