package in.succinct.bpp.core.db.model;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.routing.Config;
import in.succinct.json.JSONAwareWrapper;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class UserImpl extends ModelImpl<User> {
    public UserImpl(User proxy) {
        super(proxy);
    }
    public Map<String,String> getCredentials(boolean production, Set<String> attributes){
        User user = getProxy();
        User parent = user.getId() > 1 ?  Database.getTable(User.class).get(1) : null; // Rooot has no parent
        
        Map<String,String> finalCredentials = new HashMap<>();
        for (User u : new User[]{user,parent}){
            if (u == null){
                break;
            }
            Map<String,String> creds = AdaptorCredential.getUserCredentials(u,production,attributes);
            if (creds == null){
                //Adaptor is not enabled.
                break;
            }
            creds.forEach((k, v) -> {
                finalCredentials.putIfAbsent(k, v);
            });
        }
        return finalCredentials;
    }
    
    public boolean isSelfManaged(boolean production,Set<String> attributes){
        Map<String,String> creds = AdaptorCredential.getUserCredentials(getProxy(),production,attributes);
        return (creds == null || !creds.isEmpty());
    }
    
}
