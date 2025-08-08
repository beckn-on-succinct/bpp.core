package in.succinct.bpp.core.db.model;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;
import com.venky.swf.routing.Config;
import in.succinct.json.JSONAwareWrapper;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


public interface AdaptorCredential  extends Model {
    @UNIQUE_KEY
    @IS_NULLABLE(false)
    @PARTICIPANT
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
    
    
    static Map<String,String> getUserCredentials(User u, boolean production, Set<String> attributes){
        String adaptorName = new StringTokenizer(Config.instance().getHostName(),".").nextToken();
        
        Map<String,String> finalCredentials;
        
        AdaptorCredential adaptorCredential = Database.getTable(AdaptorCredential.class).newRecord();
        adaptorCredential.setAdaptorName(adaptorName);
        adaptorCredential.setUserId(u.getId());
        adaptorCredential.setProduction(production);
        adaptorCredential = Database.getTable(AdaptorCredential.class).getRefreshed(adaptorCredential);
        if (!adaptorCredential.getRawRecord().isNewRecord()){
            // User wants to use the adaptor
            finalCredentials = new HashMap<>();
            if (ObjectUtil.isVoid(adaptorCredential.getCredentialJson())){
                adaptorCredential.setCredentialJson("{}");
            }
            JSONObject creds = JSONAwareWrapper.parse(adaptorCredential.getCredentialJson());
            creds.forEach((k,v)->{
                if (attributes == null || attributes.contains((String)k)) {
                    if (!ObjectUtil.isVoid(v)) {
                        finalCredentials.putIfAbsent((String) k, StringUtil.valueOf(v));
                    }
                }
            });
        }else {
            finalCredentials = null;
        }
        return finalCredentials;
    }
    
}
