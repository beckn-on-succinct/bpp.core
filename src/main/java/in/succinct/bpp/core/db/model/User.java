package in.succinct.bpp.core.db.model;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.validations.Enumeration;

import java.util.List;

public interface User extends com.venky.swf.plugins.collab.db.model.user.User {
    @IS_NULLABLE
    @UNIQUE_KEY(value = "PROVIDER_ID", allowMultipleRecordsWithNull = true)
    String getProviderId();
    void setProviderId(String providerId);
    
    @IS_NULLABLE(false)
    @COLUMN_DEF(value = StandardDefault.SOME_VALUE,args = "test")
    @Enumeration("test,production")
    String getNetworkEnvironment();
    void setNetworkEnvironment(String networkEnvironment);
    
    public static User findProvider(String providerId){
        User user  = Database.getTable(User.class).newRecord();
        user.setProviderId(providerId);
        user = Database.getTable(User.class).getRefreshed(user);
        return user;
    }
    
    @IS_VIRTUAL
    String getCredentialJson();
    
    
    List<AdaptorCredential> getAdaptorCredentials();
    
}
