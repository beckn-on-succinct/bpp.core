package in.succinct.bpp.core.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import in.succinct.beckn.Note.RepresentativeAction;
import in.succinct.bpp.core.db.model.igm.Issue;
import in.succinct.bpp.core.db.model.igm.Note;

import javax.xml.crypto.Data;

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
                StringBuilder notes = new StringBuilder();
                if (ObjectUtil.isVoid(model.getResolution())){
                    model.setResolution(model.getResolutionStatus());
                }
                notes.append(model.getResolution());
                if (!ObjectUtil.isVoid(model.getResolutionRemarks())) {
                    notes.append("\nRemarks:").append(model.getResolutionRemarks());
                }

                if (!ObjectUtil.isVoid(model.getGroResolutionRemarks())) {
                    notes.append("\nGro Remarks:").append(model.getGroResolutionRemarks());
                }

                if (!ObjectUtil.isVoid(model.getDisputeResolutionRemarks())) {
                    notes.append("\nOdr Remarks:").append(model.getDisputeResolutionRemarks());
                }


                note.setNotes(notes.toString());
                note.setIssueId(model.getId());
                note.setAction(RepresentativeAction.convertor.toString(RepresentativeAction.RESOLVED));
                note.save();
            }
        }
    }
}
