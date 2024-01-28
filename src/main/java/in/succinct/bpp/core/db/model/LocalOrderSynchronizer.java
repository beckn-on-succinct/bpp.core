package in.succinct.bpp.core.db.model;

import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.plugins.background.core.DbTask;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.beckn.Amount;
import in.succinct.beckn.BecknStrings;
import in.succinct.beckn.BreakUp.BreakUpElement;
import in.succinct.beckn.BreakUp.BreakUpElement.BreakUpCategory;
import in.succinct.beckn.Cancellation;
import in.succinct.beckn.Cancellation.CancelledBy;
import in.succinct.beckn.CancellationReasons.CancellationReasonCode;
import in.succinct.beckn.Context;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Fulfillment;
import in.succinct.beckn.Fulfillment.FulfillmentStatus;
import in.succinct.beckn.Fulfillment.FulfillmentType;
import in.succinct.beckn.Intent;
import in.succinct.beckn.Issue;
import in.succinct.beckn.Issue.EscalationLevel;
import in.succinct.beckn.IssueCategory;
import in.succinct.beckn.IssueSubCategory;
import in.succinct.beckn.Item;
import in.succinct.beckn.Locations;
import in.succinct.beckn.Message;
import in.succinct.beckn.Note;
import in.succinct.beckn.Note.Notes;
import in.succinct.beckn.Option;
import in.succinct.beckn.Order;
import in.succinct.beckn.Order.Orders;
import in.succinct.beckn.Order.ReconStatus;
import in.succinct.beckn.Order.SettlementReasonCode;
import in.succinct.beckn.Order.Status;
import in.succinct.beckn.Organization;
import in.succinct.beckn.Payer;
import in.succinct.beckn.Payment;
import in.succinct.beckn.Payment.CollectedBy;
import in.succinct.beckn.Payment.CommissionType;
import in.succinct.beckn.Payment.PaymentStatus;
import in.succinct.beckn.Representative.Complainant;
import in.succinct.beckn.Request;
import in.succinct.beckn.Role;
import in.succinct.beckn.SellerException;
import in.succinct.beckn.SellerException.GenericBusinessError;
import in.succinct.beckn.SellerException.InvalidOrder;
import in.succinct.beckn.SellerException.InvalidRequestError;
import in.succinct.beckn.SettlementCorrection;
import in.succinct.beckn.SettlementDetail;
import in.succinct.beckn.SettlementDetails;
import in.succinct.beckn.Time;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.api.NetworkApiAdaptor;
import in.succinct.bpp.core.adaptor.fulfillment.FulfillmentStatusAdaptor.FulfillmentStatusAudit;
import in.succinct.bpp.core.db.model.rsp.BankAccount;
import in.succinct.bpp.core.db.model.rsp.Correction;
import in.succinct.bpp.core.db.model.rsp.Settlement;
import in.succinct.bpp.core.extensions.SuccinctIssueTracker;
import in.succinct.onet.core.adaptor.NetworkAdaptor;
import in.succinct.onet.core.adaptor.NetworkAdaptorFactory;
import in.succinct.onet.core.api.BecknIdHelper;
import in.succinct.onet.core.api.BecknIdHelper.Entity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
                if (payment.getBuyerAppFinderFeeAmount() != null) {
                    meta.setBuyerAppFinderFeeAmount(payment.getBuyerAppFinderFeeAmount());
                }
                if (payment.getBuyerAppFinderFeeType() != null) {
                    meta.setBuyerAppFinderFeeType(payment.getBuyerAppFinderFeeType().toString());
                }
                if (meta.getBuyerAppFinderFeeAmount() > 0 || meta.getBuyerAppFinderFeeType() != null){

                    BuyerAppFees fees = Database.getTable(BuyerAppFees.class).newRecord();
                    fees.setBapId(request.getContext().getBapId());
                    fees.setDomainId(request.getContext().getDomain());
                    fees.setBuyerAppFinderFeeAmount(meta.getBuyerAppFinderFeeAmount());
                    fees.setBuyerAppFinderFeeType(meta.getBuyerAppFinderFeeType());

                    TaskManager.instance().executeAsync(new FeesPersistingTask(fees,request.getContext().getTransactionId()),false);
                }
            }
        } else {
            Order order = request.getMessage().getOrder();
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
                        if (lastKnown.getFulfillment().getFulfillmentStatus().compareTo(FulfillmentStatus.Order_picked_up) >= 0){
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
                        meta.setBuyerAppFinderFeeAmount(fees.getBuyerAppFinderFeeAmount());
                        meta.setBuyerAppFinderFeeType(fees.getBuyerAppFinderFeeType());
                    }
                    if (order.getPayment() != null) {
                        if (order.getPayment().getBuyerAppFinderFeeType() != null) {
                            meta.setBuyerAppFinderFeeType(order.getPayment().getBuyerAppFinderFeeType().toString());
                        } else if (!ObjectUtil.isVoid(meta.getBuyerAppFinderFeeType())) {
                            order.getPayment().setBuyerAppFinderFeeType(CommissionType.valueOf(meta.getBuyerAppFinderFeeType()));
                            order.getPayment().setBuyerAppFinderFeeAmount(meta.getBuyerAppFinderFeeAmount());
                        }
                        if (order.getPayment().getBuyerAppFinderFeeAmount() != null) {
                            meta.setBuyerAppFinderFeeAmount(order.getPayment().getBuyerAppFinderFeeAmount());
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
                    }
                    if (order.getPayment() != null && meta.getBuyerAppFinderFeeType() != null) {
                        order.getPayment().setBuyerAppFinderFeeAmount(meta.getBuyerAppFinderFeeAmount());
                        order.getPayment().setBuyerAppFinderFeeType(CommissionType.valueOf(meta.getBuyerAppFinderFeeType()));
                    }
                    if (order.getState() == Status.Cancelled && order.getCancellation() == null && lastKnown.getCancellation() != null) {
                        order.setCancellation(lastKnown.getCancellation());
                    }
                    meta.setOrderJson(order.getInner().toString());
                    Status status = order.getState();
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
                fulfillment = order.getPrimaryFulfillment();
            }else if (order.getFulfillments().size() == 1) {
                fulfillment = order.getFulfillments().get(0);
            }
            order.setFulfillment(fulfillment);
        }

        if (fulfillment != null && fulfillment.getType() == null) {
            if (fulfillment.getEnd() == null) {
                fulfillment.setType(FulfillmentType.store_pickup);
            } else {
                fulfillment.setType(FulfillmentType.home_delivery);
            }
        }

        if (fulfillment != null) {
            if (ObjectUtil.isVoid(fulfillment.getId())) {
                fulfillment.setId("fulfillment/" + fulfillment.getType() + "/" + context.getTransactionId());
            }
            order.getFulfillments().add(fulfillment,true);
        }
    }

    public List<FulfillmentStatusAudit> getFulfillmentStatusAudit(String transactionId) {
        return getOrderMeta(transactionId).getStatusAudits();
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

    public void receiver_recon(Context rspContext , Order order, CommerceAdaptor adaptor) {
        ProviderConfig providerConfig = adaptor.getProviderConfig();
        BecknOrderMeta meta = getOrderMeta(order);
        Order lastKnown = getLastKnownOrder(meta);

        boolean ok = ObjectUtil.equals(lastKnown.getState(), order.getState());
        ok = ok && validatePayment(order, lastKnown, meta, providerConfig);
        if (!ok){
            throw new SellerException.PaymentNotSupported("Payment object has changed!");
        }

        double deliveryAmount = getDeliveryCharges(lastKnown);
        double deliveryGstFraction = (providerConfig.getDeliveryGstPct() / 100.0);

        double deliveryGST =  providerConfig.isTaxIncludedInPrice() ? (deliveryGstFraction /  (1 + deliveryGstFraction))* deliveryAmount : deliveryGstFraction * deliveryAmount;

        boolean sameState = ObjectUtil.equals(lastKnown.getFulfillment().getStart().getLocation().getAddress().getState(), lastKnown.getFulfillment().getEnd().getLocation().getAddress().getState());
        Bucket cgst = new Bucket();
        Bucket sgst = new Bucket();
        Bucket igst = new Bucket();
        if (sameState) {
            cgst.increment(deliveryGST / 2);
            sgst.increment(deliveryGST / 2);
        } else {
            igst.increment(deliveryGST);
        }

        double invoiceAmount = lastKnown.getPayment().getParams().getAmount();
        //If invoice has gst mentioned separately, TDS will be onb invoice amount - GST. For now Dec 31 2023
        // We will assume invoice is inclusive of GST and GST is not shown as separate line iten.

        TypeConverter<Double> doubleConverter = Database.getJdbcTypeHelper("").getTypeRef(Double.class).getTypeConverter();
        double feeAmount = doubleConverter.valueOf(lastKnown.getPayment().getBuyerAppFinderFeeAmount());

        if (lastKnown.getPayment().getBuyerAppFinderFeeType() == CommissionType.Percent) {
            feeAmount = (feeAmount / 100.0) * invoiceAmount;
        }


        double tcs_gst = (providerConfig.getGstWithheldPercent() / 100.0) * invoiceAmount;
        double tds =  ( providerConfig.getTaxWithheldPercent()  / 100.0 ) * invoiceAmount;
        double buyer_app_fee_gst = (providerConfig.getBuyerAppCommissionGstPct() / 100.0) * feeAmount; // Fees don't include gst.

        Payment payment = order.getPayment();
        if (payment.getStatus() != PaymentStatus.PAID || payment.getCollectedBy() != CollectedBy.BAP) {
            throw new SellerException.PaymentNotSupported();
        }

        Bucket orderAmount = new Bucket();
        for (Item item : lastKnown.getItems()) {
            orderAmount.increment(item.getPrice().getValue());
            double gstf = doubleConverter.valueOf(item.getTaxRate()) / 100.0;
            double gst = item.getPrice().getValue() * (providerConfig.isTaxIncludedInPrice()? gstf/(gstf + 1) : gstf ) ;

            if (sameState) {
                cgst.increment(gst / 2);
                sgst.increment(gst / 2);
            } else {
                igst.increment(gst);
            }
        }

        in.succinct.bpp.core.db.model.Subscriber collector = createCollector(meta,rspContext,order);
        in.succinct.bpp.core.db.model.Subscriber receiver = createReceiver(meta,rspContext,order,providerConfig);

        SettlementDetails settlementDetails1 = payment.getSettlementDetails();
        List<SettlementDetail> settlementDetails = new ArrayList<>();
        for (SettlementDetail d : settlementDetails1){
            settlementDetails.add(d);
        }
        settlementDetails.sort(Comparator.comparing(SettlementDetail::getSettlementTimestamp));

        Bucket expectedCredit = new Bucket();
        Bucket settled = new Bucket();
        for (int i = 0; i < settlementDetails.size(); i++) {
            SettlementDetail detail = settlementDetails.get(i);
            Settlement settlement = Database.getTable(Settlement.class).newRecord();
            settlement.setBecknOrderMetaId(meta.getId());
            settlement.setSequenceNumber(i);
            settlement = Database.getTable(Settlement.class).getRefreshed(settlement);

            if (!settlement.getRawRecord().isNewRecord()){
                expectedCredit.increment(settlement.getExpectedCreditInBank());
                settled.increment(settlement.getSettledAmount());
                continue;
            }
            if (i == 0) {
                settlement.setBuyerFeeAmount(feeAmount);
                    settlement.setInvoiceAmount(invoiceAmount);
                settlement.setDeliveryAmount(deliveryAmount);
                settlement.setOrderAmount(orderAmount.doubleValue());

                settlement.setCGst(cgst.doubleValue());
                settlement.setSgst(sgst.doubleValue());
                settlement.setIgst(igst.doubleValue());

                settlement.setGSTWithheld(order.getGstWithheld().getValue());
                settlement.setTDSWithheld(order.getIncomeTaxWithheld().getValue());

                settlement.setDiffAmount(order.getDiffAmount());
                settlement.setCollectionTxnId(order.getCollectionTransactionId());
                settlement.setExpectedCreditInBank(settlement.getInvoiceAmount() - settlement.getBuyerFeeAmount() - buyer_app_fee_gst - settlement.getTDSWithheld() - settlement.getGSTWithheld());
            }

            settlement.setSettledAmount(detail.getSettlementAmount());

            expectedCredit.increment(settlement.getExpectedCreditInBank());
            settled.increment(settlement.getSettledAmount());

            settlement.setReconStatus(order.getReconStatus().name());
            settlement.setOrderReconStatus(order.getOrderReconStatus().name());
            settlement.setCounterpartyDiffAmount(Math.abs(expectedCredit.doubleValue() - settled.doubleValue()));
            if (settled.doubleValue() < expectedCredit.doubleValue()){
                settlement.setCounterpartyReconStatus(ReconStatus.UNDER_PAID.name());
            }else if (settled.doubleValue() > expectedCredit.doubleValue()){
                settlement.setCounterpartyReconStatus(ReconStatus.OVER_PAID.name());
            }else {
                settlement.setCounterpartyReconStatus(ReconStatus.PAID.name());
            }
            settlement.setSettlementId(detail.getSettlementId());
            if (ObjectUtil.isVoid(settlement.getSettlementId())){
                settlement.setSettlementId(order.getSettlementId());
            }
            settlement.setSettlementReference(detail.getSettlementReference());
            settlement.setStatus(detail.getSettlementStatus().name());
            settlement.setCollectorPartyId(collector.getId());
            settlement.setReceiverPartyId(receiver.getId());
            settlement.setSettlementPhase(detail.getSettlementPhase().name());
            settlement.setSettlementReasonCode(order.getSettlementReasonCode().name());
            settlement.setSettlementType(detail.getSettlementType().name());
            settlement.save();
        }
        SettlementCorrection correction  = order.getSettlementCorrection();
        if (correction != null && correction.getDiffAmount() != null) {
            Select select = new Select().from(Correction.class);
            List<Correction> corrections = select.where(new Expression(select.getPool(),Conjunction.AND).
                    add(new Expression(select.getPool(),"BECKN_ORDER_META_ID",Operator.EQ,meta.getId()))).execute();

            Correction settlement = Database.getTable(Correction.class).newRecord();
            settlement.setBecknOrderMetaId(meta.getId());
            settlement.setSequenceNumber(corrections.size()+1);
            settlement = Database.getTable(Correction.class).getRefreshed(settlement);
            settlement.setSettledAmount(correction.getDiffAmount().getValue());
            settlement.setSettlementReference(correction.getSettlementReference());
            settlement.setReconStatus(correction.getReconStatus().name());
            settlement.setPreviousSettlementAmount(correction.getPreviousSettledAmount().getValue());
            settlement.setPreviousSettlementReference(correction.getPrevSettlementReferenceNo().toString());
            settlement.setCorrectionAmount(correction.getOrderCorrectionAmount().getValue());
            settlement.setReceiverPartyId(receiver.getId());
            settlement.setCollectorPartyId(collector.getId());
            settlement.setSettlementId(correction.getSettlementId());

            expectedCredit.increment(settlement.getExpectedCreditInBank());
            settled.increment(settlement.getSettledAmount());
            if (settled.doubleValue() < expectedCredit.doubleValue()){
                settlement.setCounterpartyReconStatus(ReconStatus.UNDER_PAID.name());
            }else{
                settlement.setCounterpartyReconStatus(ReconStatus.PAID.name());
            }

            settlement.save();
        }
        lastKnown.update(order);
        meta.setRspContextJson(rspContext.toString());
        meta.setOrderJson(lastKnown.getInner().toString());
        if (settled.doubleValue() < expectedCredit.doubleValue()){
            // All Is well.
            double diff = expectedCredit.doubleValue() - settled.doubleValue();
            Issue issue = raisePaymentIssue(rspContext,adaptor,meta , diff);
            sendOnReceiverRecon(rspContext,adaptor,meta,issue, diff);

        }

        meta.save();
    }

    private void sendOnReceiverRecon(Context rspContext, CommerceAdaptor adaptor, BecknOrderMeta meta, Issue issue, double diff) {
        //
        Request response = new Request();
        response.setContext(rspContext);
        response.setMessage(new Message());
        response.getMessage().setOrders(new Orders());
        Order order = new Order();
        Order lastKnown = new Order(meta.getOrderJson());
        order.setId(lastKnown.getId());
        order.setInvoiceNo(lastKnown.getInvoiceNo());
        order.setCollectorSubscriberId(lastKnown.getCollectorSubscriberId());
        order.setReceiverSubscriberId(lastKnown.getReceiverSubscriberId());
        order.setOrderReconStatus(lastKnown.getOrderReconStatus());
        order.setCollectionTransactionId(lastKnown.getCollectionTransactionId());
        order.setSettlementId(lastKnown.getSettlementId());
        order.setSettlementReference(lastKnown.getSettlementReference());

        order.setCounterpartyReconStatus(ReconStatus.UNDER_PAID);

        Amount diffAmount = new Amount();
        diffAmount.setValue(diff);
        diffAmount.setCurrency(lastKnown.getPayment().getParams().getCurrency());
        order.setCounterpartyDiffAmount(diffAmount);

        order.setReconMessage(new Descriptor());
        order.getReconMessage().setCode("less");
        order.getReconMessage().setName("lesser amount");

        order.setSettlementReasonCode(SettlementReasonCode.CORRECTION);
        order.setSettlementCorrection(new SettlementCorrection());

        SettlementCorrection correction = order.getSettlementCorrection();
        correction.setIssueInitiatorRef(issue.getId());
        correction.setIssueType(IssueCategory.convertor.toString(issue.getIssueCategory()));
        correction.setIssueSubtype(IssueSubCategory.convertor.toString(issue.getIssueSubCategory()));
        correction.setOrderId(order.getId());
        correction.setPrevSettlementReferenceNo(new BecknStrings());

        List<Settlement> settlements = meta.getSettlements();
        Settlement last = settlements.get(0);
        Bucket expected = new Bucket();
        Bucket actual = new Bucket();
        for (Settlement settlement : settlements) {
            expected.increment(settlement.getExpectedCreditInBank());
            actual.increment(settlement.getSettledAmount());
            correction.getPrevSettlementReferenceNo().add(settlement.getSettlementReference());
        }
        Amount orderAmount =  new Amount();
        orderAmount.setCurrency(order.getPayment().getParams().getCurrency());
        orderAmount.setValue(expected.doubleValue());
        correction.setOrderAmount(orderAmount);
        correction.setStatus(order.getState());

        correction.setOrderCorrectionAmount(orderAmount);
        Amount previousSettledAmount = new Amount();
        previousSettledAmount.setCurrency(orderAmount.getCurrency());
        previousSettledAmount.setValue(actual.doubleValue());
        correction.setPreviousSettledAmount(previousSettledAmount);

        correction.setDiffAmount(diffAmount);
        correction.setSettlementId(lastKnown.getSettlementId());
        correction.setSettlementReference(lastKnown.getSettlementReference());
        correction.setReconStatus(ReconStatus.UNDER_PAID);
        correction.setMessage(order.getReconMessage());



    }

    private Issue raisePaymentIssue(Context rspContext, CommerceAdaptor adaptor, BecknOrderMeta orderMeta, double diff) {
        Context context = new Context(orderMeta.getContextJson());
        Issue issue = new Issue();
        issue.setCreatedAt(new Date());
        issue.setId(UUID.randomUUID().toString());
        issue.setIssueCategory(IssueCategory.PAYMENT);
        issue.setIssueSubCategory(IssueSubCategory.PAYMENT_UNDER_PAID);
        issue.setEscalationLevel(EscalationLevel.ISSUE);
        issue.setExpectedResponseTime(new Time());
        issue.getExpectedResponseTime().setDuration(Duration.ofDays(2));
        issue.setExpectedResolutionTime(new Time());
        issue.getExpectedResolutionTime().setDuration(Duration.ofDays(5));
        issue.setOrder(new Order(orderMeta.getOrderJson()));
        issue.setComplainant(new Complainant());
        issue.getComplainant().setRole(Role.COMPLAINANT_PLATFORM);
        issue.getComplainant().setSubscriberId(adaptor.getSubscriber().getSubscriberId());
        issue.getComplainant().setOrganization(new Organization());
        issue.getComplainant().getOrganization().setName(adaptor.getSubscriber().getSubscriberId() + ":" + context.getDomain());
        issue.getComplainant().setPerson(adaptor.getProviderConfig().getOrganization().getAuthorizedSignatory().getPerson());
        issue.getComplainant().setContact(adaptor.getProviderConfig().getSupportContact());
        Note note = new Note();
        issue.setNotes(new Notes());
        issue.getNotes().add(note);
        note.setDescription(new Descriptor());
        note.getDescription().setLongDesc("Payment paid is short by " + diff);
        note.setCreatedAt(new Date());
        note.setCreatedBy(issue.getComplainant());
        issue.setOrder(new Order(orderMeta.getOrderJson()));
        SuccinctIssueTracker succinctIssueTracker = (SuccinctIssueTracker) adaptor.getIssueTracker();
        in.succinct.bpp.core.db.model.igm.Issue dbIssue = succinctIssueTracker.getDbIssue(context,issue);
        Issue out = SuccinctIssueTracker.getBecknIssue(dbIssue);

        //Send on_receiver_recon
        Request reply = new Request();
        reply.setContext(context);
        context.setAction("issue");
        reply.setMessage(new Message());
        reply.getMessage().setIssue(out);



        ((NetworkApiAdaptor)NetworkAdaptorFactory.getInstance().getAdaptor(orderMeta.getNetworkId()).getApiAdaptor()).callback(adaptor,reply);

        return out;

    }


    private boolean validatePayment(Order order, Order lastKnown, BecknOrderMeta meta, ProviderConfig providerConfig) {
        boolean ok = (lastKnown.getPayment().getParams().getAmount() == order.getPayment().getParams().getAmount());
        ok = ok && ObjectUtil.equals(lastKnown.getPayment().getParams().getCurrency(), order.getPayment().getParams().getCurrency());
        ok = ok && ObjectUtil.equals(lastKnown.getPayment().getUri(), order.getPayment().getUri());
        ok = ok && ObjectUtil.equals(lastKnown.getPayment().getType(), order.getPayment().getType());
        ok = ok && ObjectUtil.equals(lastKnown.getPayment().getCollectedBy(), order.getPayment().getCollectedBy());
        ok = ok && ObjectUtil.equals(lastKnown.getPayment().getBuyerAppFinderFeeType(), order.getPayment().getBuyerAppFinderFeeType());
        ok = ok && ObjectUtil.equals(lastKnown.getPayment().getBuyerAppFinderFeeAmount(), order.getPayment().getBuyerAppFinderFeeAmount());
        ok = ok && ObjectUtil.equals(lastKnown.getPayment().getReturnWindow(), order.getPayment().getReturnWindow());
        ok = ok && ObjectUtil.equals(lastKnown.getPayment().getSettlementBasis(), order.getPayment().getSettlementBasis()) || ObjectUtil.isVoid(lastKnown.getPayment().getSettlementBasis());
        ok = ok && ObjectUtil.equals(lastKnown.getPayment().getSettlementWindow(), order.getPayment().getSettlementWindow()) || ObjectUtil.isVoid(lastKnown.getPayment().getSettlementWindow());
        ok = ok && ObjectUtil.equals(lastKnown.getPayment().getWithholdingAmount(), order.getPayment().getWithholdingAmount()) || ObjectUtil.isVoid(lastKnown.getPayment().getWithholdingAmount());
        return ok;
    }
    private in.succinct.bpp.core.db.model.Subscriber createReceiver(BecknOrderMeta meta,Context rspContext ,  Order order, ProviderConfig providerConfig) {
        BankAccount receiverAccount = Database.getTable(BankAccount.class).newRecord();
        SettlementDetails details = order.getPayment().getSettlementDetails();                                                                                                                                                                                                                                              
        if (!details.isEmpty()){
            SettlementDetail detail = details.get(details.size()-1);
            receiverAccount.setAccountNo(detail.getSettlementBankAccountNo());
            receiverAccount.setAddress(detail.getBeneficiaryAddress());
            receiverAccount.setBankCode(detail.getSettlementIfscCode());
            receiverAccount.setBankName(detail.getBankName());
            receiverAccount.setName(detail.getBeneficiaryName());
            if (!ObjectUtil.equals(providerConfig.getSettlementDetail().getUpiAddress(),detail.getUpiAddress())){
                throw new SellerException.InvalidRequestError("Cannot change settlement vpa");
            }
            receiverAccount.setVirtualPaymentAddress(detail.getUpiAddress());
            receiverAccount = Database.getTable(BankAccount.class).getRefreshed(receiverAccount);
            receiverAccount.save();
        }



        in.succinct.bpp.core.db.model.Subscriber receiverSubscriber = Database.getTable(in.succinct.bpp.core.db.model.Subscriber.class).newRecord();
        Context context = new Context(meta.getContextJson());

        receiverSubscriber.setSubscriberId(order.getReceiverSubscriberId());
        if (order.getPayment().getCollectedBy().equals(CollectedBy.BAP)) {
            receiverSubscriber.setRole(in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BPP);
            receiverSubscriber.setRspSubscriberId(rspContext.getBppId());
        }else {
            receiverSubscriber.setRole(in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BAP);
            receiverSubscriber.setRspSubscriberId(rspContext.getBapId());
        }
        receiverSubscriber.setDomainId(context.getDomain());
        receiverSubscriber.setNetworkId(meta.getNetworkId());
        receiverSubscriber.setBankAccountId(receiverAccount.getId());
        receiverSubscriber  = Database.getTable(in.succinct.bpp.core.db.model.Subscriber.class).getRefreshed(receiverSubscriber);
        receiverSubscriber.save();
        return receiverSubscriber;
    }

    private in.succinct.bpp.core.db.model.Subscriber createCollector(BecknOrderMeta meta, Context rspContext , Order order) {
        BankAccount account = Database.getTable(BankAccount.class).newRecord();
        Payer payer = order.getPayer();
        account.setAccountNo(payer.getAccountNo());
        account.setAddress(payer.getAddress());
        account.setBankCode(payer.getBankCode());
        account.setBankName(payer.getBankName());
        account.setName(payer.getName());
        account.setVirtualPaymentAddress(payer.getVpa());
        account = Database.getTable(BankAccount.class).getRefreshed(account);
        account.save();

        in.succinct.bpp.core.db.model.Subscriber collectorSubscriber = Database.getTable(in.succinct.bpp.core.db.model.Subscriber.class).newRecord();
        collectorSubscriber.setSubscriberId(order.getCollectorSubscriberId());

        if (order.getPayment().getCollectedBy().equals(CollectedBy.BAP)) {
            collectorSubscriber.setRole(in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BAP);
            collectorSubscriber.setRspSubscriberId(rspContext.getBapId());
        }else {
            collectorSubscriber.setRole(in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BPP);
            collectorSubscriber.setRspSubscriberId(rspContext.getBppId());
        }
        Context context = new Context(meta.getContextJson());
        collectorSubscriber.setDomainId(context.getDomain());
        collectorSubscriber.setNetworkId(meta.getNetworkId());
        collectorSubscriber.setBankAccountId(account.getId());
        collectorSubscriber = Database.getTable(in.succinct.bpp.core.db.model.Subscriber.class).getRefreshed(collectorSubscriber);
        collectorSubscriber.save();
        return collectorSubscriber;
    }

    public List<Settlement> getSettlements(String becknTransactionId) {
        return getOrderMeta(becknTransactionId).getSettlements();
    }

    public String getTransactionId(Order order) {
        BecknOrderMeta meta = getOrderMeta(order);
        return meta.getBecknTransactionId();
    }
}
























