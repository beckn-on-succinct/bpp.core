package in.succinct.bpp.core.db.model.logistics;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.WATERMARK;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.user.User;

public interface CarrierAccount extends Model {
    @UNIQUE_KEY
    @COLUMN_DEF(StandardDefault.CURRENT_USER)
    @PARTICIPANT
    @HIDDEN
    Long getOwnerId();
    void setOwnerId(Long id);
    User getOwner();
    
    @UNIQUE_KEY
    @IS_NULLABLE(value = false)
    Long getCarrierId();
    void setCarrierId(Long id);
    Carrier getCarrier();
    
    
    @WATERMARK("Copy api Key from porter's portal")
    String getCredentials();
    void setCredentials(String credentials);
    //Understod by the adaptor.
    
    
    
}
