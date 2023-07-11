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
        RepresentativeAction action = RepresentativeAction.valueOf(model.getAction());
        Representative loggedByRepresentor = model.getLoggedByRepresentor();
        Application application = loggedByRepresentor.getSubscriber().getApplication();
        if (ObjectUtil.isVoid(model.getNoteId())){
            model.setNoteId(UUID.randomUUID().toString());
        }

        Event event = Event.find("on_create_issue_note");
        if (event != null && application != null){
            TaskManager.instance().executeAsync((DbTask)()-> event.raise(application,SuccinctIssueTracker.getBecknIssue(model.getIssue())),false);
        }
    }
}
