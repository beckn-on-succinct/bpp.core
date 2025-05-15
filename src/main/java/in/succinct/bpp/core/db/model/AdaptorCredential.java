package in.succinct.bpp.core.db.model;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;


public interface AdaptorCredential  extends Model {
    @UNIQUE_KEY
    @IS_NULLABLE(false)
    Long getUserId();
    void setUserId(Long id);
    User getUser();
    
    
    @UNIQUE_KEY
    String getAdaptorName();
    void setAdaptorName(String adaptorName);
    
    String getCredentialJson();
    void setCredentialJson(String credentialJson);
}
