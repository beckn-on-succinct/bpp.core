package in.succinct.bpp.core.db.model.igm;

import com.venky.swf.db.annotations.column.ATTRIBUTE_GROUP;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.relationship.CONNECTED_VIA;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import in.succinct.bpp.core.db.model.igm.config.Odr;

import java.sql.Timestamp;
import java.util.List;

public interface Issue extends Model {

    @UNIQUE_KEY
    @Index
    public String getIssueId();
    public void setIssueId(String issueId);


    @Index
    @Enumeration(enumClass = "in.succinct.beckn.IssueCategory")
    public String getIssueCategory();
    public void setIssueCategory(String category);

    @Index
    @Enumeration(enumClass = "in.succinct.beckn.IssueSubCategory")
    public String getIssueSubCategory();
    public void setIssueSubCategory(String category);

    @PROTECTION
    @IS_NULLABLE
    public Boolean isSatisfactorilyClosed();
    public void setSatisfactorilyClosed(Boolean satisfactorilyClosed);

    @PROTECTION
    @Index
    @Enumeration(enumClass = "in.succinct.beckn.Issue$Status")
    public String getStatus();
    public void setStatus(String status);

    public List<Note> getNotes();

    public Long getFinalizedOdrId();
    public void setFinalizedOdrId(Long id);
    public Odr getFinalizedOdr();


    @Index
    public Timestamp getExpectedResponseTs();
    public void setExpectedResponseTs(Timestamp expectedResponseTs);

    @Index
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
    @Enumeration(enumClass = "in.succinct.beckn.Resolution$ResolutionStatus")
    @Index
    public String getResolutionStatus();
    public void setResolutionStatus(String resolutionStatus);

    @ATTRIBUTE_GROUP("Resolution")
    @Enumeration(enumClass = "in.succinct.beckn.Resolution$ResolutionAction")
    @IS_NULLABLE
    @Index
    public String getResolutionAction();
    public void setResolutionAction(String resolutionAction);


    public List<Representative> getRepresentors();

    @PARTICIPANT
    @Index
    public Long getComplainantId();
    public void setComplainantId(Long id);
    public Representative getComplainant();

    @PARTICIPANT
    @Index
    public Long getRespondentId();
    public void setRespondentId(Long id);
    public Representative getRespondent();


    public Long getResolutionProviderId();
    public void setResolutionProviderId(Long id);
    public Representative getResolutionProvider();


    @Enumeration(enumClass = "in.succinct.beckn.Issue$EscalationLevel")
    public String getEscalationLevel();
    public void setEscalationLevel(String escalationLevel);



    public Long getCascadedFromIssueId();
    public void setCascadedFromIssueId(Long id);
    public Issue getCascadedFromIssue();


    @CONNECTED_VIA("CASCADED_FROM_ISSUE_ID")
    List<Issue> getCascadedIssues();


    @COLUMN_SIZE(4096)
    @Index
    public String getOrderJson();
    public void setOrderJson(String orderJson);


    @IS_NULLABLE
    public Double getRefundAmount();
    public void setRefundAmount(Double refundAmount);
}
