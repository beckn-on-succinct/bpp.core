package in.succinct.bpp.core.db.model;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.background.core.DbTask;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.beckn.BreakUp.BreakUpElement;
import in.succinct.beckn.BreakUp.BreakUpElement.BreakUpCategory;
import in.succinct.beckn.Cancellation;
import in.succinct.beckn.Cancellation.CancelledBy;
import in.succinct.beckn.CancellationReasons.CancellationReasonCode;
import in.succinct.beckn.Context;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Fee;
import in.succinct.beckn.Fulfillment;
import in.succinct.beckn.Fulfillment.FulfillmentStatus;
import in.succinct.beckn.Fulfillment.RetailFulfillmentType;
import in.succinct.beckn.Intent;
import in.succinct.beckn.Locations;
import in.succinct.beckn.Option;
import in.succinct.beckn.Order;
import in.succinct.beckn.Order.Status;
import in.succinct.beckn.Payment;
import in.succinct.beckn.Payments;
import in.succinct.beckn.Request;
import in.succinct.beckn.SellerException;
import in.succinct.beckn.SellerException.GenericBusinessError;
import in.succinct.beckn.SellerException.InvalidOrder;
import in.succinct.beckn.SellerException.InvalidRequestError;
import in.succinct.beckn.Tracking;
import in.succinct.onet.core.adaptor.NetworkAdaptor;
import in.succinct.onet.core.api.BecknIdHelper;
import in.succinct.onet.core.api.BecknIdHelper.Entity;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalOrderSynchronizer {

    final Subscriber subscriber;

    LocalOrderSynchronizer(Subscriber subscriber) {
        this.subscriber = subscriber;
    }


    Map<String, BecknOrderMeta> map = new HashMap<>();

    private Map<String, BecknOrderMeta> getOrderMetaMap() {
        return map;
    }

    private String getCurrentBecknTransactionId() {
        Set<String> transactionIds = getBecknTransactionIds();
        if (transactionIds.size() == 1) {
            return transactionIds.iterator().next();
        }
        return null;
    }

    private BecknOrderMeta getOrderMeta() {
        String currentTransactionId = getCurrentBecknTransactionId();

        return getOrderMeta(currentTransactionId);
    }

    private BecknOrderMeta getOrderMeta(String transactionId) {
        return getOrderMeta(transactionId,false);
    }
    private BecknOrderMeta getOrderMeta(String transactionId,boolean throwIfNotKnown) {
        if (transactionId == null) {
            throw new GenericBusinessError("Unable to find the transaction");
        }
        Map<String, BecknOrderMeta> map = getOrderMetaMap();
        BecknOrderMeta meta = map.get(transactionId);
        if (meta == null) {
            Select select = new Select().from(BecknOrderMeta.class);
            select.where(new Expression(select.getPool(), Conjunction.AND).
                    add(new Expression(select.getPool(),"BECKN_TRANSACTION_ID", Operator.EQ, transactionId)).
                    add(new Expression(select.getPool(),"SUBSCRIBER_ID", Operator.EQ, subscriber.getSubscriberId()))).orderBy("ID DESC");
            List<BecknOrderMeta> list = select.execute(1);

            if (list.isEmpty()) {
                if (throwIfNotKnown){
                    throw new GenericBusinessError("Unable to find the transaction");
                }
                meta = Database.getTable(BecknOrderMeta.class).newRecord();
                meta.setSubscriberId(subscriber.getSubscriberId());
                meta.setBecknTransactionId(transactionId);
                meta.setOrderJson("{}");
                meta.setStatusUpdatedAtJson("{}");
            }else {
                meta = list.get(0);
            }

            map.put(transactionId, meta);
        }
        return meta;
    }

    private BecknOrderMeta getOrderMeta(Order order) {
        BecknOrderMeta meta = Database.getTable(BecknOrderMeta.class).newRecord();
        meta.setBapOrderId(order.getId());
        meta = Database.getTable(BecknOrderMeta.class).getRefreshed(meta);

        return meta;
    }

    private Set<String> getBecknTransactionIds() {
        Map<String, BecknOrderMeta> map = getOrderMetaMap();
        return map.keySet();
    }

    public String getLocalOrderId(String transactionId) {
        return getOrderMeta(transactionId).getECommerceOrderId();
    }

    public String getLocalDraftOrderId(String transactionId) {
        return getOrderMeta(transactionId).getECommerceDraftOrderId();
    }

    public String getLocalOrderId(Order order) {
        if (!ObjectUtil.isVoid(order.getId())) {
            for (BecknOrderMeta value : getOrderMetaMap().values()) {
                if (ObjectUtil.equals(value.getBapOrderId(), order.getId())) {
                    return value.getECommerceOrderId();
                }
            }
            BecknOrderMeta meta = getOrderMeta(order);
            if (meta != null) {
                getOrderMetaMap().put(meta.getBecknTransactionId(), meta);
                return meta.getECommerceOrderId();
            }
        }

        return getOrderMeta().getECommerceOrderId();
    }

    public String getLocalDraftOrderId(Order order) {
        if (!ObjectUtil.isVoid(order.getId())) {
            for (BecknOrderMeta value : getOrderMetaMap().values()) {
                if (ObjectUtil.equals(value.getBapOrderId(), order.getId())) {
                    return value.getECommerceDraftOrderId();
                }
            }
            BecknOrderMeta meta = getOrderMeta(order);
            if (meta != null) {
                getOrderMetaMap().put(meta.getBecknTransactionId(), meta);
                return meta.getECommerceDraftOrderId();
            }
        }
        return getOrderMeta().getECommerceDraftOrderId();
    }

    public void setLocalOrderId(String transactionId, String localOrderid) {
        getOrderMeta(transactionId).setECommerceOrderId(localOrderid);
    }

    public void setLocalDraftOrderId(String transactionId, String localDraftOrderid) {
        getOrderMeta(transactionId).setECommerceDraftOrderId(localDraftOrderid);
    }

    public Order getLastKnownOrder(String transactionId) {
        return getLastKnownOrder(transactionId,false);
    }
    public Order getLastKnownOrder(String transactionId, boolean throwIfNotKnown) {
        return getLastKnownOrder(getOrderMeta(transactionId,throwIfNotKnown));
    }

    public void sync(String transactionId,Order order){
        sync(transactionId,order,false);
    }
    public void sync(String transactionId,Order order,boolean persist){
        BecknOrderMeta meta =getOrderMeta(transactionId);
        sync(meta,order);
        if (persist) {
            meta.save();
        }
    }
    private void sync(BecknOrderMeta meta, Order order){
        meta.setOrderJson(order.getInner().toString());
    }

    private Order getLastKnownOrder(BecknOrderMeta meta) {
        return new Order(meta.getOrderJson());
    }


    public static class FeesPersistingTask implements DbTask{
        BuyerAppFees fees;
        String searchTransactionId;
        public FeesPersistingTask(BuyerAppFees fees , String searchTransactionId){
            this.fees = fees;
            this.searchTransactionId = searchTransactionId;
        }
        @Override
        public void execute() {
            fees = Database.getTable(BuyerAppFees.class).getRefreshed(fees);
            if (fees.getRawRecord().isNewRecord()){
                synchronized (FeesPersistingTask.class){
                    fees = Database.getTable(BuyerAppFees.class).getRefreshed(fees);
                    saveFees(fees,searchTransactionId);
                }
            }else{
                saveFees(fees,searchTransactionId);
            }
        }
        public static void saveFees(BuyerAppFees fees,String searchTransactionId){
            if (fees.getRawRecord().isNewRecord() || fees.isDirty()) {
                fees.setSearchTransactionId(searchTransactionId);
                fees.save();
            }
        }
    }
    public void sync(Request request, NetworkAdaptor adaptor, boolean persist) {
        if (request == null || request.getMessage() == null) {
            return;
        }

        BecknOrderMeta meta = getOrderMeta(request.getContext().getTransactionId());
        if (meta.getRawRecord().isNewRecord()) {
            meta.setContextJson(request.getContext().toString());
        }
        if (ObjectUtil.isVoid(meta.getNetworkId())) {
            meta.setNetworkId(adaptor.getId());
        } else if (!ObjectUtil.equals(adaptor.getId(), meta.getNetworkId())) {
            throw new InvalidRequestError();
        }
        Intent intent = request.getMessage().getIntent();
        if (intent != null) {
            Payment payment = intent.getPayment();
            if (payment != null) {
                Fee fee = payment.getBuyerFinderFee();
                if (fee != null){
                    meta.setFinderFeeJson(fee.getInner().toString());
                }
                BuyerAppFees fees = Database.getTable(BuyerAppFees.class).newRecord();
                fees.setBapId(request.getContext().getBapId());
                fees.setDomainId(request.getContext().getDomain());
                fees.setFinderFeeJson(meta.getFinderFeeJson());

                TaskManager.instance().executeAsync(new FeesPersistingTask(fees,request.getContext().getTransactionId()),false);
            }
        } else {
            Order order = request.getMessage().getOrder();
            Tracking tracking = request.getMessage().getTracking();
            if (order == null && !ObjectUtil.isVoid(request.getMessage().getOrderId())) {
                order = new Order();
                order.setId(request.getMessage().getOrderId());
                request.getMessage().setOrder(order);
            }
            if (order != null) {
                Order lastKnown = new Order(meta.getOrderJson());
                String action = request.getContext().getAction();
                if (Subscriber.BPP_ACTION_SET.contains(action)) {
                    if ("cancel".equals(action)){
                        if (request.getMessage().getCancellationReasonCode() != null){
                            if (order.getCancellation() == null){
                                order.setCancellation( new Cancellation());
                                order.getCancellation().setSelectedReason(new Option());
                                order.getCancellation().getSelectedReason().setDescriptor(new Descriptor());
                                order.getCancellation().getSelectedReason().getDescriptor().setCode(CancellationReasonCode.convertor.toString(request.getMessage().getCancellationReasonCode()));
                                order.getCancellation().setCancelledBy(CancelledBy.BUYER);
                            }
                        }
                        if (lastKnown.getFulfillment().getFulfillmentStatus().compareTo(FulfillmentStatus.In_Transit) >= 0){
                            throw new SellerException.CancellationNotPossible("Order already closed!");
                        }
                    }
                    // Incoming
                    if (!ObjectUtil.isVoid(order.getId())) {
                        if (ObjectUtil.isVoid(meta.getBapOrderId())) {
                            meta.setBapOrderId(order.getId());
                        } else if (!ObjectUtil.equals(meta.getBapOrderId(), order.getId())) {
                            throw new InvalidOrder("Transaction associated with a different network order");
                        }
                    }
                    fixFulfillment(request.getContext(), order);
                    if (order.getProviderLocation() == null){
                        order.setProviderLocation(lastKnown.getProviderLocation());
                    }
                    if (order.getProvider() == null){
                        order.setProvider(lastKnown.getProvider());
                    }
                    fixLocation(order);

                    BuyerAppFees fees = Database.getTable(BuyerAppFees.class).newRecord();
                    //fees.setSearchTransactionId(request.getContext().getTransactionId());
                    fees.setBapId(request.getContext().getBapId());
                    fees.setDomainId(request.getContext().getDomain());
                    fees = Database.getTable(BuyerAppFees.class).getRefreshed(fees);
                    if (!fees.getRawRecord().isNewRecord()) {
                        fees = Database.getTable(BuyerAppFees.class).lock(fees.getId());
                        meta.setFinderFeeJson(fees.getFinderFeeJson());
                    }
                    Payments payments = order.getPayments();
                    Payment payment = payments == null || payments.isEmpty() ? null : payments.get(0);
                    if (payment != null) {
                        Fee fee = payment.getBuyerFinderFee();
                        if (fee != null) {
                            meta.setFinderFeeJson(payment.getBuyerFinderFee().toString());
                        } else if (!ObjectUtil.isVoid(meta.getFinderFeeJson())) {
                            payment.setBuyerFinderFee(new Fee(meta.getFinderFeeJson()));
                        }
                        if (fee != null) {
                            meta.setFinderFeeJson(fee.toString());
                        }
                    }
                } else {
                    if (order.getId() == null) {
                        if (meta.getBapOrderId() != null) {
                            order.setId(meta.getBapOrderId());
                        } else if (meta.getECommerceOrderId() != null && action.equals("on_confirm")) {
                            order.setId(BecknIdHelper.getBecknId(meta.getECommerceOrderId(), subscriber, Entity.order));
                            meta.setBapOrderId(order.getId());
                        }
                    }else {
                        if (ObjectUtil.isVoid(meta.getECommerceOrderId()) && ObjectUtil.equals(action,"on_confirm")){
                            meta.setECommerceOrderId(order.getId());
                            meta.setBapOrderId(order.getId());
                        }
                    }
                    if (tracking != null){
                        if (ObjectUtil.isVoid(meta.getTrackingUrl())){
                            meta.setTrackingUrl(tracking.getUrl());
                        }
                    }
                    
                    Payments payments = order.getPayments();
                    Payment payment = payments == null || payments.isEmpty() ? null : payments.get(0);
                    if (payment != null && meta.getFinderFeeJson() != null) {
                        payment.setBuyerFinderFee(new Fee(meta.getFinderFeeJson()));
                    }
                    if (order.getStatus() == Status.Cancelled && order.getCancellation() == null && lastKnown.getCancellation() != null) {
                        order.setCancellation(lastKnown.getCancellation());
                    }
                    meta.setOrderJson(order.getInner().toString());
                    Status status = order.getStatus();
                    if (request.getContext().getTimestamp() == null){
                        request.getContext().setTimestamp(new Date());
                    }

                    if (order.getUpdatedAt() != null && order.getUpdatedAt().after(request.getContext().getTimestamp())){
                        request.getContext().setTimestamp(order.getUpdatedAt());
                    }
                    if (status != null) {
                        meta.setStatusReachedAt(status, request.getContext().getTimestamp());
                    }
                    Fulfillment fulfillment = order.getFulfillment();
                    if (fulfillment != null) {
                        FulfillmentStatus fulfillmentStatus = fulfillment.getFulfillmentStatus();
                        if (fulfillmentStatus != null) {
                            meta.setFulfillmentStatusReachedAt(fulfillmentStatus, request.getContext().getTimestamp());
                        }
                    }
                }
            }
        }
        meta.save();


    }

    public void setFulfillmentStatusReachedAt(String transactionId, FulfillmentStatus status, Date at, boolean persist) {
        BecknOrderMeta meta = getOrderMeta(transactionId);
        meta.setFulfillmentStatusReachedAt(status, at);
        if (persist) {
            meta.save();
        }
    }
    public void setStatusReachedAt(String transactionId, Status status, Date at, boolean persist) {
        BecknOrderMeta meta = getOrderMeta(transactionId);
        meta.setStatusReachedAt(status, at);
        if (persist) {
            meta.save();
        }
    }
    public void fixLocation(Order order) {
        if (order.getProvider() == null) {
            return;
        }

        Locations locations = order.getProvider().getLocations();
        if (locations == null) {
            locations = new Locations();
            order.getProvider().setLocations(locations);
        }
        if (order.getProviderLocation() != null) {
            if (locations.get(order.getProviderLocation().getId()) == null) {
                locations.add(order.getProviderLocation());
            }
        }
        if (locations.size() == 1) {
            order.setProviderLocation(locations.get(0));
        }
    }

    public void fixFulfillment(Context context, Order order) {
        if (order.getFulfillments() == null) {
            order.setFulfillments(new in.succinct.beckn.Fulfillments());
        }

        Fulfillment fulfillment = order.getFulfillment();

        if (fulfillment == null) {
            //if (order.getFulfillments().size() > 1) {
            //throw new InvalidRequestError("Multiple fulfillments for order!");
            //} else
            if (order.getFulfillments().size() > 1) {
                fulfillment = order.getFulfillment();
            } else if (order.getFulfillments().size() == 1) {
                fulfillment = order.getFulfillments().get(0);
            }
            if (fulfillment != null){
                order.setFulfillment(fulfillment);
            }
        }

        if (fulfillment != null && fulfillment.getType() == null) {
            if (fulfillment._getEnd() == null) {
                fulfillment.setType(RetailFulfillmentType.store_pickup.toString());
            } else {
                fulfillment.setType(RetailFulfillmentType.home_delivery.toString());
            }
        }

        if (fulfillment != null) {
            if (ObjectUtil.isVoid(fulfillment.getId())) {
                fulfillment.setId("fulfillment/" + fulfillment.getType() + "/" + context.getTransactionId());
            }
            order.getFulfillments().add(fulfillment,true);
        }
    }

    public boolean hasOrderReached(String transactionId, Status status){
        return getOrderMeta(transactionId).getStatusReachedAt(status) != null;
    }

    public Date getStatusReachedAt(String transactionId, Status status){
        return getOrderMeta(transactionId).getStatusReachedAt(status);
    }

    public String getTrackingUrl(Order order) {
        if (!ObjectUtil.isVoid(order.getId())) {
            for (BecknOrderMeta value : getOrderMetaMap().values()) {
                if (ObjectUtil.equals(value.getBapOrderId(), order.getId())) {
                    return value.getTrackingUrl();
                }
            }
        }
        return getOrderMeta().getTrackingUrl();

    }

    public String getTrackingUrl(String transactionId) {
        return getOrderMeta(transactionId).getTrackingUrl();
    }

    public void setTrackingUrl(String transactionId, String trackingUrl) {
        getOrderMeta(transactionId).setTrackingUrl(trackingUrl);
    }
    private double getDeliveryCharges(Order order){
        BreakUpElement deliveryElement = null;
        for (BreakUpElement e : order.getQuote().getBreakUp()) {
            if (e.getType() == BreakUpCategory.delivery) {
                deliveryElement = e;
                break;
            }
        }
        return deliveryElement == null ? 0 : deliveryElement.getPrice().getValue();
    }

    public String getTransactionId(Order order) {
        BecknOrderMeta meta = getOrderMeta(order);
        return meta.getBecknTransactionId();
    }
}
























