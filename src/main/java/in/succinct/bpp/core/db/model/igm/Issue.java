package in.succinct.bpp.core.db.model.igm;

import com.venky.swf.db.annotations.column.ATTRIBUTE_GROUP;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import in.succinct.bpp.core.db.model.igm.config.Odr;

import java.sql.Timestamp;
import java.util.List;

public interface Issue extends Model {

    @UNIQUE_KEY
    public String getIssueId();
    public void setIssueId(String issueId);


    @Enumeration(enumClass = "in.succinct.beckn.IssueCategory")
    public String getIssueCategory();
    public void setIssueCategory(String category);

    @Enumeration(enumClass = "in.succinct.beckn.IssueSubCategory")
    public String getIssueSubCategory();
    public void setIssueSubCategory(String category);

    public boolean isSatisfactorilyClosed();
    public void setSatisfactorilyClosed(boolean satisfactorilyClosed);

    @Enumeration(enumClass = "in.succinct.beckn.Status")
    public String getStatus();
    public void setStatus(String status);

    public List<Note> getNotes();

    public Long getFinalizedOdrId();
    public void setFinalizedOdrId(Long id);
    public Odr getFinalizedOdr();


    public Timestamp getExpectedResponseTs();
    public void setExpectedResponseTs(Timestamp expectedResponseTs);

    public Timestamp getExpectedResolutionTs();
    public void setExpectedResolutionTs(Timestamp expectedResolutionTs);


    @ATTRIBUTE_GROUP("Resolution")
    public String getResolution();
    public void setResolution(String resolution);

    @ATTRIBUTE_GROUP("Resolution")
    public String getResolutionRemarks();
    public void setResolutionRemarks(String resolutionRemarks);

    @ATTRIBUTE_GROUP("Resolution")
    public String getGroResolutionRemarks();
    public void setGroResolutionRemarks(String groResolutionRemarks);

    @ATTRIBUTE_GROUP("Resolution")
    public String getDisputeResolutionRemarks();
    public void setDisputeResolutionRemarks(String disputeResolutionRemarks);

    @ATTRIBUTE_GROUP("Resolution")
    @Enumeration(enumClass = "in.succinct.beckn.igm.Resolution.ResolutionStatus")
    public String getResolutionStatus();
    public void setResolutionStatus(String resolutionStatus);

    @ATTRIBUTE_GROUP("Resolution")
    @Enumeration(enumClass = "in.succinct.beckn.Resolution.ResolutionAction")
    public String getResolutionAction();
    public void setResolutionAction(String resolutionAction);


    public List<Representative> getRepresentors();

    public Long getComplainantId();
    public void setComplainantId(Long id);
    public Representative getComplainant();

    public Long getRespondentId();
    public void setRespondentId(Long id);
    public Representative getRespondent();


    public Long getResolutionProviderId();
    public void setResolutionProviderId(Long id);
    public Representative getResolutionProvider();


    @Enumeration(enumClass = "in.succinct.beckn.EscalationLevel")
    public String getEscalationLevel();
    public void setEscalationLevel(String escalationLevel);



    public Long getCascadedFromIssueId();
    public void setCascadedFromIssueId(Long id);
    public Issue getCascadedFromIssue();


    @CONNECTED_VIA("CASCADED_FROM_ISSUE_ID")
    List<Issue> getCascadedIssues();


    @COLUMN_SIZE(4096)
    public String getOrderJson();
    public void setOrderJson(String orderJson);


}
