package in.succinct.bpp.core.db.model.rsp;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;

public interface BankAccount extends Model {
    @UNIQUE_KEY
    public String getName();
    public void setName(String Name);

    public String getAddress();
    public void setAddress(String address);

    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    public String getAccountNo();
    public void setAccountNo(String AccountNo);

    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    public String getBankCode();
    public void setBankCode(String BankCode);

    @UNIQUE_KEY("VPA")
    @IS_NULLABLE
    public String getVirtualPaymentAddress();
    public void setVirtualPaymentAddress(String VirtualPaymentAddress);

    public String getBankName();
    public void setBankName(String bankName);

    public String getBranchName();
    public void setBranchName(String branchName);

}
