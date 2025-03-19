package in.succinct.bpp.core.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import in.succinct.bpp.core.db.model.logistics.CarrierAccount;

import java.util.List;

public class CarrierAccountParticipantExtension extends ParticipantExtension<CarrierAccount> {
    static {
        registerExtension(new CarrierAccountParticipantExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, CarrierAccount partiallyFilledModel, String fieldName) {
        if (ObjectUtil.equals(fieldName,"OWNER_ID")){
            return List.of(user.getId());
        }
        return null;
    }
}
