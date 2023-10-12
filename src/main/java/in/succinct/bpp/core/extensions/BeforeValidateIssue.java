package in.succinct.bpp.core.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.model.User;
import in.succinct.beckn.Issue.EscalationLevel;
import in.succinct.beckn.Issue.Status;
import in.succinct.beckn.Note.RepresentativeAction;
import in.succinct.beckn.Resolution.ResolutionStatus;
import in.succinct.beckn.Role;
import in.succinct.bpp.core.db.model.igm.Issue;
import in.succinct.bpp.core.db.model.igm.Note;
import in.succinct.bpp.core.db.model.igm.Representative;

public class BeforeValidateIssue extends BeforeModelValidateExtension<Issue> {
    static {
        registerExtension(new BeforeValidateIssue());
    }
    @Override
    public void beforeValidate(Issue model) {
        if (model.getRawRecord().isFieldDirty("RESOLUTION_STATUS") || model.getRawRecord().isFieldDirty("RESOLUTION_ACTION") || model.getRawRecord().isFieldDirty("RESOLUTION")){
            if (!ObjectUtil.isVoid(model.getResolutionStatus())){
                Note note = Database.getTable(Note.class).newRecord();
                note.setLoggedByRepresentorId(model.getRespondentId());
                if (ObjectUtil.isVoid(model.getResolution())){
                    model.setResolution(model.getResolutionStatus());
                }
                note.setSummary(model.getResolution());

                switch (EscalationLevel.valueOf(model.getEscalationLevel())){
                    case ISSUE:
                        note.setNotes(model.getResolutionRemarks());break;
                    case GRIEVANCE:
                        note.setNotes(model.getGroResolutionRemarks()); break;
                    case DISPUTE:
                        note.setNotes(model.getDisputeResolutionRemarks());break;
                }

                note.setIssueId(model.getId());
                note.setAction(RepresentativeAction.convertor.toString(RepresentativeAction.RESOLVED));
                note.save();
            }
        }
        if (ObjectUtil.isVoid(model.getEscalationLevel())){
            model.setEscalationLevel(EscalationLevel.ISSUE.name());
        }else if (model.getRawRecord().isFieldDirty("ESCALATION_LEVEL")){
            model.setStatus(Status.OPEN.name());
            model.setResolutionStatus(null);
        }


        EscalationLevel currentEscalationLevel = EscalationLevel.valueOf(model.getEscalationLevel());

        Role requiredRespondentRole;
        if (currentEscalationLevel == EscalationLevel.ISSUE){
            requiredRespondentRole = Role.RESPONDENT_PLATFORM;
        }else {
            requiredRespondentRole = Role.RESPONDENT_GRO;
        }

        for (Representative representor : model.getRepresentors()) {
            Role representorRole = Role.valueOf(representor.getRole());

            if (requiredRespondentRole == representorRole ){
                model.setRespondentId(representor.getId());
                break;
            }
        }
    }
}
