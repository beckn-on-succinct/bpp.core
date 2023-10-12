package in.succinct.bpp.core.db.model.igm;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;

import java.util.List;

public interface Note extends Model {
    @IS_NULLABLE(false)
    public long getIssueId();
    public void setIssueId(long id);
    public Issue getIssue();

    @UNIQUE_KEY
    public String getNoteId();
    public void setNoteId(String noteId);

    @IS_NULLABLE
    public Long getParentNoteId();
    public void setParentNoteId(Long parentNoteId);
    public Note getParentNote();

    public String getNotes();
    public void setNotes(String notes);

    public String getSummary();
    public void setSummary(String summary);

    List<NoteAttachment> getAttachments();

    @Enumeration(enumClass = "in.succinct.beckn.Note$RepresentativeAction")
    public String getAction();
    public void setAction(String action);


    @PARTICIPANT(redundant = true)
    @PROTECTION(Kind.NON_EDITABLE)
    public Long getLoggedByRepresentorId();
    public void setLoggedByRepresentorId(Long id);
    public Representative getLoggedByRepresentor();

    public static Note find(String notId){
        return find(notId,Note.class);
    }
    public static <T extends Note> T find(String notId, Class<T> clazz){
        T dbNote = Database.getTable(clazz).newRecord();
        dbNote.setNoteId(notId);
        dbNote = Database.getTable(clazz).find(dbNote,true);
        return dbNote;
    }
}
