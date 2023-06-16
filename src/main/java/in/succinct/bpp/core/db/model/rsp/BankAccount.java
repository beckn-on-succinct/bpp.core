package in.succinct.bpp.core.db.model.rsp;

import com.venky.swf.db.model.Model;

public interface BankAccount extends Model {
    public String getName();
    public void setName(String Name);

    public String getAddress();
    public void setAddress(String Address);

    public String getAccountNo();
    public void setAccountNo(String AccountNo);

    public String getBankCode();
    public void setBankCode(String BankCode);
    
    public String getVirtualPaymentAddress();
    public void setVirtualPaymentAddress(String VirtualPaymentAddress);

    public String getBankName();
    public void setBankName(String bankName);

    public String getBranchName();
    public void setBranchName(String branchName);

}
