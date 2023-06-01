package in.succinct.bpp.core.db.model.igm;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.attachment.db.model.Attachment;

import java.util.List;

public interface Note extends Model {
    public Long getIssueId();
    public void setIssueId(Long id);
    public Issue getIssue();

    @UNIQUE_KEY
    public String getNoteId();
    public void setNoteId(String noteId);

    @COLUMN_DEF(StandardDefault.ZERO)
    public Long getParentNoteId();
    public void setParentNoteId(Long parentNoteId);

    public String getNotes();
    public void setNotes(String notes);

    List<Attachment> getAttachments();

    @Enumeration(enumClass = "in.succinct.beckn.Note.Action")
    public String getAction();
    public void setAction(String action);


    public Long getLoggedByRepresentorId();
    public void setLoggedByRepresentorId(Long id);
    public Representative getLoggedByRepresentor();
}
