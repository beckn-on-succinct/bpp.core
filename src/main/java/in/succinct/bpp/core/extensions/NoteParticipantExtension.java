package in.succinct.bpp.core.extensions;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import in.succinct.bpp.core.db.model.igm.Note;
import in.succinct.bpp.core.db.model.igm.Representative;

import java.util.List;
import java.util.Optional;

public class NoteParticipantExtension extends ParticipantExtension<Note> {
    static {
        registerExtension(new NoteParticipantExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, Note partiallyFilledModel, String fieldName) {
        if (fieldName.equalsIgnoreCase("LOGGED_BY_REPRESENTOR_ID")){
            Optional<Representative> o = partiallyFilledModel.getIssue().getRepresentors().stream().
                    filter(r-> (r.getUserId() == user.getId())).findAny();

            o.ifPresent(representative -> partiallyFilledModel.setLoggedByRepresentorId(representative.getId()));
        }
        return null;
    }
}
