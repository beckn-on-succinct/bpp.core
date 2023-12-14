package in.succinct.bpp.core.extensions;

import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelSaveExtension;
import in.succinct.beckn.Note.RepresentativeAction;
import in.succinct.beckn.Role;
import in.succinct.bpp.core.db.model.igm.Issue;
import in.succinct.bpp.core.db.model.igm.Note;

import java.util.UUID;

public class BeforeSaveIssue extends BeforeModelSaveExtension<Issue> {
    static {
        registerExtension(new BeforeSaveIssue());
    }
    @Override
    public void beforeSave(Issue issue) {
        if (issue.getRespondentId() != null && issue.getRawRecord().isFieldDirty("RESPONDENT_ID")) {
            if (Role.valueOf(issue.getRespondent().getRole()) == Role.RESPONDENT_PLATFORM) {
                Note note = Database.getTable(Note.class).newRecord();
                note.setNotes("We received your issue and we will get back to you.");
                note.setAction(RepresentativeAction.PROCESSING.name());
                note.setIssueId(issue.getId());
                note.setNoteId(UUID.randomUUID().toString());
                note.setLoggedByRepresentorId(issue.getRespondentId());
                note.save();
            }
        }
    }
}
