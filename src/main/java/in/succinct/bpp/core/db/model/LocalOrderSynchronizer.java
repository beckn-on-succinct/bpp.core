package in.succinct.bpp.core.db.model;

import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.JdbcTypeHelper.TypeConverter;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import in.succinct.beckn.BreakUp.BreakUpElement;
import in.succinct.beckn.BreakUp.BreakUpElement.BreakUpCategory;
import in.succinct.beckn.Context;
import in.succinct.beckn.Fulfillment;
import in.succinct.beckn.Fulfillment.FulfillmentStatus;
import in.succinct.beckn.Fulfillment.FulfillmentType;
import in.succinct.beckn.Intent;
import in.succinct.beckn.Item;
import in.succinct.beckn.Locations;
import in.succinct.beckn.Order;
import in.succinct.beckn.Order.ReconStatus;
import in.succinct.beckn.Order.Status;
import in.succinct.beckn.Payer;
import in.succinct.beckn.Payment;
import in.succinct.beckn.Payment.CollectedBy;
import in.succinct.beckn.Payment.CommissionType;
import in.succinct.beckn.Payment.PaymentStatus;
import in.succinct.beckn.Request;
import in.succinct.beckn.SellerException;
import in.succinct.beckn.SellerException.CancellationNotPossible;
import in.succinct.beckn.SellerException.GenericBusinessError;
import in.succinct.beckn.SellerException.InvalidOrder;
import in.succinct.beckn.SellerException.InvalidRequestError;
import in.succinct.beckn.SettlementCorrection;
import in.succinct.beckn.SettlementDetail;
import in.succinct.beckn.SettlementDetails;
import in.succinct.bpp.core.adaptor.NetworkAdaptor;
import in.succinct.bpp.core.adaptor.api.BecknIdHelper;
import in.succinct.bpp.core.adaptor.api.BecknIdHelper.Entity;
import in.succinct.bpp.core.adaptor.fulfillment.FulfillmentStatusAdaptor.FulfillmentStatusAudit;
import in.succinct.bpp.core.db.model.rsp.BankAccount;
import in.succinct.bpp.core.db.model.rsp.Settlement;

import java.util.ArrayList;
import java.util.Comparator;
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
        if (transactionId == null) {
            throw new GenericBusinessError("Unable to find the transaction");
        }
        Map<String, BecknOrderMeta> map = getOrderMetaMap();
        BecknOrderMeta meta = map.get(transactionId);
        if (meta == null) {
            meta = Database.getTable(BecknOrderMeta.class).newRecord();
            meta.setBecknTransactionId(transactionId);
            meta.setSubscriberId(subscriber.getSubscriberId());
            meta = Database.getTable(BecknOrderMeta.class).getRefreshed(meta);

            if (meta.getRawRecord().isNewRecord()) {
                meta.setOrderJson("{}");
                meta.setStatusUpdatedAtJson("{}");
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
        return getLastKnownOrder(getOrderMeta(transactionId));
    }

    public Order getLastKnownOrder(BecknOrderMeta meta) {
        return new Order(meta.getOrderJson());
    }

    public void sync(Request request, NetworkAdaptor adaptor, boolean persist) {
        if (request == null || request.getMessage() == null) {
            return;
        }

        BecknOrderMeta meta = getOrderMeta(request.getContext().getTransactionId());
        meta.setContextJson(request.getContext().toString());
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
            }
        } else {
            Order order = request.getMessage().getOrder();
            if (order == null && !ObjectUtil.isVoid(request.getMessage().getOrderId())) {
                order = new Order();
                order.setId(request.getMessage().getOrderId());
            }
            if (order != null) {
                Order lastKnown = new Order(meta.getOrderJson());
                String action = request.getContext().getAction();
                if (Subscriber.BPP_ACTION_SET.contains(action)) {
                    if ("cancel".equals(action)){
                        if (lastKnown.getFulfillment().getFulfillmentStatus().compareTo(FulfillmentStatus.Order_picked_up) >= 0){
                            throw new SellerException.CancellationNotPossible("Order already shipped!");
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
                    fixLocation(order);

                    lastKnown.setProviderLocation(order.getProviderLocation());
                    lastKnown.setBilling(order.getBilling());
                    if (action.equals("cancel")) {
                        if (order.getCancellation() != null) {
                            lastKnown.setCancellation(order.getCancellation());
                        }
                    }

                    if (order.getFulfillment() != null) {
                        lastKnown.setFulfillment(new Fulfillment());
                        lastKnown.getFulfillment().update(order.getFulfillment());
                    }
                    if (order.getPayment() != null){
                        lastKnown.setPayment(new Payment());
                        lastKnown.getPayment().update(order.getPayment());
                    }

                    meta.setOrderJson(lastKnown.getInner().toString());

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
                    if (status != null) {
                        meta.setStatusReachedAt(status, new Date(System.currentTimeMillis()));
                    }
                    Fulfillment fulfillment = order.getFulfillment();
                    if (fulfillment != null) {
                        FulfillmentStatus fulfillmentStatus = fulfillment.getFulfillmentStatus();
                        if (fulfillmentStatus != null) {
                            meta.setFulfillmentStatusReachedAt(fulfillmentStatus, new Date(System.currentTimeMillis()));
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
            if (order.getFulfillments().size() > 1) {
                throw new InvalidRequestError("Multiple fulfillments for order!");
            } else if (order.getFulfillments().size() == 1) {
                fulfillment = order.getFulfillments().get(0);
                order.setFulfillment(fulfillment);
            }
        }

        if (fulfillment != null && fulfillment.getType() == null) {
            if (fulfillment.getEnd() == null) {
                fulfillment.setType(FulfillmentType.store_pickup);
            } else {
                fulfillment.setType(FulfillmentType.home_delivery);
            }
        }


        order.setFulfillments(new in.succinct.beckn.Fulfillments());
        if (fulfillment != null) {
            if (fulfillment.getType() == null) {
                fulfillment.setType(FulfillmentType.home_delivery);
            }
            if (ObjectUtil.isVoid(fulfillment.getId())) {
                fulfillment.setId("fulfillment/" + fulfillment.getType() + "/" + context.getTransactionId());
            }
            order.getFulfillments().add(fulfillment);
        }
    }

    public List<FulfillmentStatusAudit> getFulfillmentStatusAudit(String transactionId) {
        return getOrderMeta(transactionId).getStatusAudits();
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

    public void receiver_recon(Order order, ProviderConfig providerConfig) {
        BecknOrderMeta meta = getOrderMeta(order);
        Order lastKnown = getLastKnownOrder(meta);

        boolean ok = ObjectUtil.equals(lastKnown.getState(), order.getState());
        ok = ok && validatePayment(order, lastKnown, meta, providerConfig);
        if (!ok){
            return;
        }

        double deliveryAmount = getDeliveryCharges(lastKnown);
        double deliveryGST = (providerConfig.getDeliveryGstPct() / 100.0) * deliveryAmount;

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

        TypeConverter<Double> doubleConverter = Database.getJdbcTypeHelper("").getTypeRef(Double.class).getTypeConverter();
        double feeAmount = doubleConverter.valueOf(lastKnown.getPayment().getBuyerAppFinderFeeAmount());

        if (lastKnown.getPayment().getBuyerAppFinderFeeType() == CommissionType.Percent) {
            feeAmount = (feeAmount / 100.0) * invoiceAmount;
        }


        double tcs_gst = (providerConfig.getGstWithheldPercent() / 100.0) * invoiceAmount;
        double tds = providerConfig.getTaxWithheldPercent() * invoiceAmount / 100.0;
        double buyer_app_fee_gst = (providerConfig.getBuyerAppCommissionGstPct() / 100.0) * feeAmount;

        Payment payment = order.getPayment();
        if (payment.getStatus() != PaymentStatus.PAID || payment.getCollectedBy() != CollectedBy.BAP) {
            throw new SellerException.PaymentNotSupported();
        }

        Bucket orderAmount = new Bucket();
        for (Item item : lastKnown.getItems()) {
            orderAmount.increment(item.getPrice().getValue());
            double gst = item.getPrice().getValue() * doubleConverter.valueOf(item.getTags().get("tax_rate")) / 100.0;
            if (sameState) {
                cgst.increment(gst / 2);
                sgst.increment(gst / 2);
            } else {
                igst.increment(gst);
            }
        }

        in.succinct.bpp.core.db.model.Subscriber collector = createCollector(meta,order);
        in.succinct.bpp.core.db.model.Subscriber receiver = createReceiver(meta,order,providerConfig);

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
                settlement.setCGst(cgst.doubleValue());
                settlement.setSgst(sgst.doubleValue());
                settlement.setIgst(igst.doubleValue());

                settlement.setDeliveryAmount(deliveryAmount);
                settlement.setOrderAmount(orderAmount.doubleValue());
                settlement.setGSTWithheld(order.getGstWithheld().getValue());
                settlement.setTDSWithheld(order.getIncomeTaxWithheld().getValue());

                settlement.setDiffAmount(order.getDiffAmount());
                settlement.setCollectionTxnId(order.getCollectionTransactionId());
                settlement.setExpectedCreditInBank(settlement.getInvoiceAmount() - settlement.getBuyerFeeAmount() - settlement.getTDSWithheld() - settlement.getGSTWithheld());
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
            Settlement settlement = Database.getTable(Settlement.class).newRecord();
            settlement.setBecknOrderMetaId(meta.getId());
            settlement.setSequenceNumber(9999);
            settlement = Database.getTable(Settlement.class).getRefreshed(settlement);
            settlement.setSettledAmount(correction.getDiffAmount());
            settlement.setSettlementReference(correction.getSettlementReference());
            settlement.setReconStatus(correction.getReconStatus().name());
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
        meta.setOrderJson(lastKnown.getInner().toString());
        meta.save();
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
    private in.succinct.bpp.core.db.model.Subscriber createReceiver(BecknOrderMeta meta, Order order,ProviderConfig providerConfig) {
        BankAccount account = Database.getTable(BankAccount.class).newRecord();
        SettlementDetails details = order.getPayment().getSettlementDetails();                                                                                                                                                                                                                                              
        if (details.size() > 0){
            SettlementDetail detail = details.get(details.size()-1);
            account.setAccountNo(detail.getSettlementBankAccountNo());                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          
            account.setAddress(detail.getBeneficiaryAddress());
            account.setBankCode(detail.getSettlementIfscCode());
            account.setBankName(detail.getBankName());
            account.setName(detail.getBeneficiaryName());
            if (!ObjectUtil.equals(providerConfig.getVPA(),detail.getUpiAddress())){
                throw new SellerException.InvalidRequestError("Cannot change settlement vpa");
            }
            account.setVirtualPaymentAddress(detail.getUpiAddress());
            account = Database.getTable(BankAccount.class).getRefreshed(account);
            account.save();
        }



        in.succinct.bpp.core.db.model.Subscriber subscriber = Database.getTable(in.succinct.bpp.core.db.model.Subscriber.class).newRecord();
        Context context = new Context(meta.getContextJson());

        if (order.getPayment().getCollectedBy().equals(CollectedBy.BAP)) {
            subscriber.setSubscriberId(context.getBppId());
            subscriber.setRole(in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BPP);
        }else {
            subscriber.setSubscriberId(context.getBapId());                                                                                                                                                         
            subscriber.setRole(in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BAP);
        }
        subscriber.setDomainId(context.getDomain());
        subscriber.setNetworkId(meta.getNetworkId());
        subscriber.setRspSubscriberId(order.getReceiverSubscriberId());
        subscriber.setBankAccountId(account.getId());
        subscriber  = Database.getTable(in.succinct.bpp.core.db.model.Subscriber.class).getRefreshed(subscriber);
        subscriber.save();
        return subscriber;
    }

    private in.succinct.bpp.core.db.model.Subscriber createCollector(BecknOrderMeta meta, Order order) {
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

        in.succinct.bpp.core.db.model.Subscriber subscriber = Database.getTable(in.succinct.bpp.core.db.model.Subscriber.class).newRecord();
        Context context = new Context(meta.getContextJson());
        if (order.getPayment().getCollectedBy().equals(CollectedBy.BAP)) {
            subscriber.setSubscriberId(context.getBapId());
            subscriber.setRole(in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BAP);
        }else {
            subscriber.setSubscriberId(context.getBppId());
            subscriber.setRole(in.succinct.beckn.Subscriber.SUBSCRIBER_TYPE_BPP);
        }
        subscriber.setDomainId(context.getDomain());
        subscriber.setNetworkId(meta.getNetworkId());
        subscriber.setRspSubscriberId(order.getCollectorSubscriberId());
        subscriber.setBankAccountId(account.getId());
        subscriber = Database.getTable(in.succinct.bpp.core.db.model.Subscriber.class).getRefreshed(subscriber);
        subscriber.save();
        return subscriber;
    }

    public List<Settlement> getSettlements(String becknTransactionId) {
        return getOrderMeta(becknTransactionId).getSettlements();
    }

    public String getTransactionId(Order order) {
        BecknOrderMeta meta = getOrderMeta(order);
        return meta.getBecknTransactionId();
    }
}
























