package in.succinct.bpp.core.db.model.logistics;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

public interface Carrier extends Model {
    @UNIQUE_KEY
    String getName();
    void setName(String name);
    
    public static Carrier load(String name) {
        Carrier c  = Database.getTable(Carrier.class).newRecord();
        c.setName(name);
        c= Database.getTable(Carrier.class).getRefreshed(c);
        if (c.getRawRecord().isNewRecord()){
            c.save();
        }
        return c;
    }
}
