package in.succinct.bpp.core.db.model;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.model.Model;


public interface AdaptorCredential  extends Model {
    @UNIQUE_KEY
    @IS_NULLABLE(false)
    Long getUserId();
    void setUserId(Long id);
    User getUser();
    
    @UNIQUE_KEY
    @COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)
    boolean isProduction();
    void setProduction(boolean production);
    
    @UNIQUE_KEY
    String getAdaptorName();
    void setAdaptorName(String adaptorName);
    
    String getCredentialJson();
    void setCredentialJson(String credentialJson);
}
