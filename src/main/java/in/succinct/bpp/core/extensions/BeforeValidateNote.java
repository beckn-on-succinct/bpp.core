package in.succinct.bpp.core.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.Event;
import com.venky.swf.plugins.background.core.DbTask;
import com.venky.swf.plugins.background.core.TaskManager;
import in.succinct.beckn.Note.RepresentativeAction;
import in.succinct.bpp.core.db.model.igm.Note;
import in.succinct.bpp.core.db.model.igm.Representative;

import java.util.UUID;

public class BeforeValidateNote extends BeforeModelValidateExtension<Note> {
    static {
        registerExtension(new BeforeValidateNote());
    }
    @Override
    public void beforeValidate(Note model) {
        if (ObjectUtil.isVoid(model.getNoteId())){
            model.setNoteId(UUID.randomUUID().toString());
        }


    }
}
