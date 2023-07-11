package in.succinct.bpp.core.db.model.igm;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.plugins.attachment.db.model.Attachment;

public interface NoteAttachment extends Attachment {
    @IS_NULLABLE(false)
    public Long getNoteId();
    public void setNoteId(Long id);
    public Note getNote();


}
