package in.succinct.bpp.core.db.model;

import com.venky.swf.db.table.ModelImpl;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.Fulfillment.FulfillmentStatus;
import in.succinct.beckn.Order.Status;

import java.util.Date;

public class BecknOrderMetaImpl extends ModelImpl<BecknOrderMeta> {

    public BecknOrderMetaImpl(BecknOrderMeta meta){
        super(meta);
    }
    public BecknObject getStatusAudit(){
        String json = getProxy().getStatusUpdatedAtJson();
        if (json == null){
            json = "{}";
        }
        BecknObject object = new BecknObject(json);
        return object;
    }

    public Date getStatusReachedAt(Status status){
        BecknObject sa = getStatusAudit();
        return sa.getTimestamp(status.toString());
    }
    public void setStatusReachedAt(Status status, Date at){
        BecknObject sa = getStatusAudit();
        Date statusReachedAt = sa.getTimestamp(status.toString());
        if (statusReachedAt == null || at.before(statusReachedAt)) {
            sa.set(status.toString(), at, BecknObject.TIMESTAMP_FORMAT_WITH_MILLS);
            getProxy().setStatusUpdatedAtJson(sa.getInner().toString());
        }
    }

    public Date getFulfillmentStatusReachedAt(FulfillmentStatus status){
        BecknObject sa = getStatusAudit();
        BecknObject fsa = sa.get(BecknObject.class,"fulfillmentStatusAudit",true);
        return fsa.getTimestamp(status.toString());
    }
    public void setFulfillmentStatusReachedAt(FulfillmentStatus status, Date at){
        BecknObject sa = getStatusAudit();
        BecknObject fsa = sa.get(BecknObject.class,"fulfillmentStatusAudit",true);

        Date statusReachedAt = fsa.getTimestamp(status.toString());
        if (statusReachedAt == null || at.before(statusReachedAt)) {
            fsa.set(status.toString(), at, BecknObject.TIMESTAMP_FORMAT_WITH_MILLS);
            getProxy().setStatusUpdatedAtJson(sa.getInner().toString());
        }
    }
}
