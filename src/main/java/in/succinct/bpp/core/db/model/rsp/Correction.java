package in.succinct.bpp.core.db.model.rsp;

import com.venky.swf.db.model.Model;
import in.succinct.bpp.core.db.model.BecknOrderMeta;
import in.succinct.bpp.core.db.model.igm.Issue;

public interface Correction extends Settlement {
    public Long getBecknOrderMetaId();
    public void setBecknOrderMetaId(Long id);
    public BecknOrderMeta getBecknOrderMeta();

    public Long getIssueId();
    public void setIssueId(Long id);
    public Issue getIssue();

    
    public double getCorrectionAmount();
    public void setCorrectionAmount(double correctionAmount);

    public double getPreviousSettlementAmount();
    public void setPreviousSettlementAmount(double previousSettlementAmount);

    public String getPreviousSettlementReference();
    public void setPreviousSettlementReference(String previousSettlementReference);





}
