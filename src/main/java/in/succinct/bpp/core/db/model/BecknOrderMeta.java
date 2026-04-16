package in.succinct.bpp.core.db.model;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.audit.db.model.AUDITED;
import com.venky.swf.plugins.audit.db.model.JSONDiff;
import in.succinct.beckn.Fulfillment.FulfillmentStatus;
import in.succinct.beckn.Order.Status;
import in.succinct.bpp.core.db.model.rsp.Settlement;
import in.succinct.events.PaymentStatusEvent;

import java.util.Date;
import java.util.List;

@AUDITED
public interface BecknOrderMeta extends Model {
    @UNIQUE_KEY("bt")
    @Index
    public String getBecknTransactionId();
    public void setBecknTransactionId(String becknTransactionId);

    @UNIQUE_KEY("bt")
    @Index
    public String getSubscriberId();
    public void setSubscriberId(String subscriberId);

    public String getNetworkId();
    public void setNetworkId(String networkId);
    
    String getProviderId();
    void setProviderId(String providerId);

    @IS_VIRTUAL
    String getPayLoad();
    void setPayLoad(String payload);


    @Index
    @UNIQUE_KEY(value = "bap_order")
    public String getBapOrderId();
    public void setBapOrderId(String eCommerceOrderId);


    @Index
    @UNIQUE_KEY(value = "do")
    @IS_NULLABLE
    public String getECommerceDraftOrderId();
    public void setECommerceDraftOrderId(String eCommerceDraftOrderId);

    @Index
    @UNIQUE_KEY(value = "eo")
    public String getECommerceOrderId();
    public void setECommerceOrderId(String eCommerceOrderId);

    @COLUMN_SIZE(2048*64)
    @IS_NULLABLE
    @JSONDiff
    public String getOrderJson();
    public void setOrderJson(String orderJson);


    @COLUMN_SIZE(2048*4)
    @IS_NULLABLE
    @JSONDiff
    public String getContextJson();
    public void setContextJson(String contextJson);
    
    @COLUMN_SIZE(1024)
    @IS_NULLABLE
    @JSONDiff
    String getFinderFeeJson();
    void setFinderFeeJson(String finderFeeJson);

    @COLUMN_SIZE(2048*4)
    @IS_NULLABLE
    @JSONDiff
    public String getStatusUpdatedAtJson();
    public void setStatusUpdatedAtJson(String statusUpdatedAtJson);


    public Date getStatusReachedAt(Status status);
    public void setStatusReachedAt(Status status, Date at);

    public Date getFulfillmentStatusReachedAt(FulfillmentStatus status);
    public void setFulfillmentStatusReachedAt(FulfillmentStatus status, Date at);

    @COLUMN_SIZE(1024)
    public String getTrackingUrl();
    public void setTrackingUrl(String trackingUrl);

    @COLUMN_SIZE(2048*4)
    @IS_NULLABLE
    public String getRspContextJson();
    public void setRspContextJson(String rspContextJson);
    
    @Index
    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    boolean isArchived();
    void setArchived(boolean archived);
    
    public void createPaymentLink();
    public void updatePayment(PaymentStatusEvent event);
    
    public void summarize(boolean force);

    public List<Settlement>  getSettlements();
}
