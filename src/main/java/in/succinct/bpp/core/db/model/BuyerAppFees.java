package in.succinct.bpp.core.db.model;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.audit.db.model.AUDITED;

@AUDITED
public interface BuyerAppFees extends Model {
    @UNIQUE_KEY
    public String getDomainId();
    public void setDomainId(String domainId);

    @UNIQUE_KEY
    public String getBapId();
    public void setBapId(String bapId);



    public String getSearchTransactionId();
    public void setSearchTransactionId(String searchTransactionId);
    
    @COLUMN_SIZE(1024)
    @IS_NULLABLE
    String getFinderFeeJson();
    void setFinderFeeJson(String finderFeeJson);

    
    
    
    
}
