package in.succinct.bpp.core.adaptor.fulfillment;

import in.succinct.beckn.BecknObject;
import in.succinct.beckn.Fulfillment.FulfillmentStatus;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;

import java.util.Date;
import java.util.List;
import java.util.Map;

public abstract class FulfillmentStatusAdaptor {
    final CommerceAdaptor commerceAdaptor;
    protected FulfillmentStatusAdaptor(CommerceAdaptor commerceAdaptor){
        this.commerceAdaptor = commerceAdaptor;
    }

    public CommerceAdaptor getCommerceAdaptor() {
        return commerceAdaptor;
    }



    public abstract Map<String,String> getAuthHeaders(boolean refresh);
    public abstract List<FulfillmentStatusAudit> getStatusAudit (String orderNumber);
    public abstract String getTrackingUrl(String orderNumber);

    public static class FulfillmentStatusAudit extends BecknObject {
        public FulfillmentStatusAudit(){
        }
        public FulfillmentStatus getFulfillmentStatus(){
            return getEnum(FulfillmentStatus.class, "fulfillment_status");
        }
        public void setFulfillmentStatus(FulfillmentStatus fulfillment_status){
            setEnum("fulfillment_status",fulfillment_status);
        }

        public Date getDate(){
            return getTimestamp("date");
        }
        public void setDate(Date date){
            set("date",date,TIMESTAMP_FORMAT);
        }

    }
}
