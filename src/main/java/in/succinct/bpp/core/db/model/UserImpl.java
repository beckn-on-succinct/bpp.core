package in.succinct.bpp.core.db.model;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.routing.Config;

import java.util.StringTokenizer;

public class UserImpl extends ModelImpl<User> {
    public UserImpl(User proxy) {
        super(proxy);
    }
    public String getCredentialJson(){
        String adaptorName = new StringTokenizer(Config.instance().getHostName(),".").nextToken();

        AdaptorCredential adaptorCredential = Database.getTable(AdaptorCredential.class).newRecord();
        adaptorCredential.setAdaptorName(adaptorName);
        adaptorCredential.setUserId(getProxy().getId());
        adaptorCredential = Database.getTable(AdaptorCredential.class).getRefreshed(adaptorCredential);
        if (ObjectUtil.isVoid(adaptorCredential.getCredentialJson())){
            adaptorCredential.setCredentialJson("{}");
        }
        return adaptorCredential.getCredentialJson();
    }
}
