package in.succinct.bpp.core.adaptor;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import in.succinct.beckn.Cancellation;
import in.succinct.beckn.Cancellation.CancelledBy;
import in.succinct.beckn.CancellationReasons.CancellationReasonCode;
import in.succinct.beckn.Catalog;
import in.succinct.beckn.Categories;
import in.succinct.beckn.Context;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Fulfillment;
import in.succinct.beckn.Fulfillment.FulfillmentType;
import in.succinct.beckn.Fulfillment.ServiceablityTags;
import in.succinct.beckn.Fulfillments;
import in.succinct.beckn.Images;
import in.succinct.beckn.Items;
import in.succinct.beckn.Location;
import in.succinct.beckn.Locations;
import in.succinct.beckn.Message;
import in.succinct.beckn.Option;
import in.succinct.beckn.Order;
import in.succinct.beckn.Payment;
import in.succinct.beckn.Payment.CollectedBy;
import in.succinct.beckn.Payment.PaymentType;
import in.succinct.beckn.Payments;
import in.succinct.beckn.Provider;
import in.succinct.beckn.Providers;
import in.succinct.beckn.Request;
import in.succinct.beckn.SellerException;
import in.succinct.beckn.SellerException.InvalidOrder;
import in.succinct.beckn.SellerException.InvalidRequestError;
import in.succinct.beckn.SellerException.TrackingNotSupported;
import in.succinct.beckn.SellerException.UpdationNotPossible;
import in.succinct.beckn.Tag;
import in.succinct.beckn.Tracking;
import in.succinct.bpp.core.adaptor.api.BecknIdHelper;
import in.succinct.bpp.core.adaptor.api.BecknIdHelper.Entity;
import in.succinct.bpp.core.adaptor.fulfillment.FulfillmentStatusAdaptor.FulfillmentStatusAudit;
import in.succinct.bpp.core.adaptor.igm.IssueTracker;
import in.succinct.bpp.core.db.model.LocalOrderSynchronizerFactory;
import in.succinct.bpp.core.db.model.ProviderConfig;
import in.succinct.bpp.core.db.model.ProviderConfig.DeliveryRules;
import org.json.simple.JSONArray;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractCommerceAdaptor extends CommerceAdaptor{


    public AbstractCommerceAdaptor(Map<String,String> configuration, Subscriber subscriber) {
        super(configuration,subscriber);
    }


    public Fulfillments getFulfillments() {
        Fulfillments fulfillments = new Fulfillments();
        if (getProviderConfig().isStorePickupSupported()) {
            fulfillments.add(getStorePickup());
        }
        if (getProviderConfig().isHomeDeliverySupported()){
            fulfillments.add(getHomeDelivery());
        }
        return fulfillments;
    }

    protected Fulfillment getStorePickup(){
        Fulfillment cFulfillment = new Fulfillment();
        cFulfillment.setType(FulfillmentType.store_pickup);
        cFulfillment.setId(BecknIdHelper.getBecknId("1",getSubscriber(), Entity.fulfillment));
        cFulfillment.setContact(getProviderConfig().getSupportContact());
        return cFulfillment;
    }

    protected Fulfillment getHomeDelivery(){
        Fulfillment cFulfillment = new Fulfillment();
        cFulfillment.setType(FulfillmentType.home_delivery);
        cFulfillment.setId(BecknIdHelper.getBecknId("2",getSubscriber(), Entity.fulfillment));
        cFulfillment.setContact(getProviderConfig().getSupportContact());
        cFulfillment.setProviderName(getProviderConfig().getFulfillmentProviderName());
        cFulfillment.setTracking(false);
        cFulfillment.setCategory(getProviderConfig().getFulfillmentCategory().toString());
        cFulfillment.setTAT(getProviderConfig().getFulfillmentTurnAroundTime());

        cFulfillment.setServiceablityTags(new ServiceablityTags());

        ServiceablityTags serviceabilityTags = cFulfillment.getServiceablityTags();


        in.succinct.beckn.List serviceabilityTagDetail = new in.succinct.beckn.List();
        serviceabilityTags.add(new Tag("serviceability",serviceabilityTagDetail));

        DeliveryRules rules = getProviderConfig().getDeliveryRules();
        if (rules == null || rules.size() == 0) {
            serviceabilityTagDetail.add(new Tag("location", getProviderConfig().getLocation().getId()));
            serviceabilityTagDetail.add(new Tag("category", getProviderConfig().getCategory().getId()));
            serviceabilityTagDetail.add(new Tag("type", "12"));
            serviceabilityTagDetail.add(new Tag("val", "IND"));
            serviceabilityTagDetail.add(new Tag("unit", "Country"));
        }else {
            serviceabilityTagDetail.add(new Tag("location", getProviderConfig().getLocation().getId()));
            serviceabilityTagDetail.add(new Tag("category", getProviderConfig().getCategory().getId()));
            serviceabilityTagDetail.add(new Tag("type", "10"));
            serviceabilityTagDetail.add(new Tag("val", rules.get(0).getMaxDistance()));
            serviceabilityTagDetail.add(new Tag("unit", "km"));
        }
        return cFulfillment;

    }



    public Provider getProvider() {
        ProviderConfig config = getProviderConfig();
        Provider provider = new Provider();
        provider.setDescriptor(new Descriptor());
        provider.getDescriptor().setName(config.getStoreName());
        provider.getDescriptor().setCode(getSubscriber().getAppId());
        provider.getDescriptor().setShortDesc(config.getStoreName());
        provider.getDescriptor().setLongDesc(config.getStoreName());
        provider.getDescriptor().setImages(new Images());
        provider.getDescriptor().setSymbol(config.getLogo());
        provider.getDescriptor().getImages().add(config.getLogo());

        provider.setId(getSubscriber().getAppId()); // Provider is same as subscriber.!!
        provider.setTtl(120);
        if (!ObjectUtil.isVoid(config.getFssaiRegistrationNumber())) {
            provider.setFssaiLicenceNo(config.getFssaiRegistrationNumber());
        }
        provider.setPayments(getSupportedPaymentCollectionMethods());
        provider.setLocations(getProviderLocations());
        provider.setFulfillments(getFulfillments());
        provider.setItems(getItems());
        provider.setCategoryId(config.getCategory().getId());
        provider.setCategories(new Categories());
        provider.getCategories().add(getProviderConfig().getCategory());
        //provider.setTime(config.getTime());
        return provider;
    }


    public Payments getSupportedPaymentCollectionMethods(){
        Payments payments = new Payments();
        Payment payment = new Payment();
        payment.setId(BecknIdHelper.getBecknId("1",getSubscriber(), Entity.payment));
        payment.setType(PaymentType.ON_ORDER);
        payment.setCollectedBy(CollectedBy.BAP);
        payments.add(payment);
        if (getProviderConfig().isCodSupported()){
            payment = new Payment();
            payment.setId(BecknIdHelper.getBecknId("2",getSubscriber(), Entity.payment));
            payment.setType(PaymentType.POST_FULFILLMENT);
            payment.setCollectedBy(CollectedBy.BPP);
            payments.add(payment);
        }
        return payments;
    }
    public void fixFulfillment(Context context, Order order){
        if (order.getFulfillments() == null) {
            order.setFulfillments(new in.succinct.beckn.Fulfillments());
        }
        Map<FulfillmentType, in.succinct.beckn.Fulfillment> map = new HashMap<>();
        for (in.succinct.beckn.Fulfillment f :getFulfillments()) {
            map.put(f.getType(),f);
        }



        Fulfillment fulfillment = order.getFulfillment();

        if (fulfillment == null) {
            if (order.getFulfillments().size() > 1) {
                throw new InvalidRequestError("Multiple fulfillments for order!");
            } else if (order.getFulfillments().size() == 1) {
                order.setFulfillment(order.getFulfillments().get(0));
            } else {
                order.setFulfillment(map.get(FulfillmentType.store_pickup));
            }
            fulfillment = order.getFulfillment();
        }

        if (fulfillment != null && fulfillment.getType() == null){
            if (fulfillment.getEnd() == null && map.containsKey(FulfillmentType.store_pickup)) {
                fulfillment.setType(FulfillmentType.store_pickup);
            }else if (map.containsKey(FulfillmentType.home_delivery)){
                fulfillment.setType(FulfillmentType.home_delivery);
            }else {
                fulfillment = null;
                order.setFulfillment(null);
                order.getFulfillments().setInner(new JSONArray());
            }
        }

        if (fulfillment != null && fulfillment.getId() == null){
            fulfillment.setId("fulfillment/"+fulfillment.getType()+"/"+context.getTransactionId());
        }

        order.setFulfillments(new Fulfillments());
        if (fulfillment != null){
            fulfillment.rm("id");
            Fulfillment configFulfillment = map.get(fulfillment.getType());
            fulfillment.update(configFulfillment);
            fulfillment.rm("id");
            fulfillment.setId("fulfillment/"+fulfillment.getType()+"/"+context.getTransactionId());
            order.getFulfillments().add(fulfillment);
        }
    }
    public void fixLocation(Order order){
        Locations allLocations = getProviderLocations();
        Locations locations = order.getProvider().getLocations();
        if (locations == null){
            locations = new Locations();
            order.getProvider().setLocations(locations);
        }
        if (order.getProviderLocation() != null){
            if (locations.get(order.getProviderLocation().getId()) == null){
                locations.add(order.getProviderLocation());
            }
        }
        for (Location location : locations){
            Location actualLocation = allLocations.get(location.getId());
            location.rm("id");
            location.update(actualLocation);
        }
        if (locations.size() == 1 ){
            order.setProviderLocation(locations.get(0));
        }
    }

    public abstract Locations getProviderLocations();
    public abstract Items getItems();
    public abstract boolean isTaxIncludedInPrice() ;
    public abstract Order initializeDraftOrder(Request request) ;
    public abstract Order confirmDraftOrder(Order draftOrder) ;
    public abstract Order getStatus(Order order);
    public abstract Order cancel(Order order) ;
    public abstract String getTrackingUrl(Order order) ;
    public abstract List<FulfillmentStatusAudit> getStatusAudit (Order order);


    public void _search(Request reply) {
        Message message = new Message();
        reply.setMessage(message);

        Catalog catalog = new Catalog();
        message.setCatalog(catalog);

        Providers providers = new Providers();
        catalog.setProviders(providers);
        Provider provider = getProvider();
        providers.add(provider);

    }

    public void confirm(Request request, Request reply){
        Order draftOrder = request.getMessage().getOrder();
        Order confirmedOrder = confirmDraftOrder(draftOrder);
        reply.getMessage().setOrder(confirmedOrder);
    }


    public abstract void search(Request request, Request reply);
    public abstract void select(Request request, Request reply) ;

    public void init(Request request, Request reply) {

        Message message = new Message();
        reply.setMessage(message);

        Order draftOrder = initializeDraftOrder(request);
        message.setOrder(draftOrder);
    }

    public void track(Request request, Request reply) {
        /* Take track message and fill response with on_track message */
        Order order = request.getMessage().getOrder();
        if (order == null){
            if (request.getMessage().getOrderId() != null){
                order = new Order();
                order.setId(request.getMessage().getOrderId());
                request.getMessage().setOrder(order);
            }
        }
        if (order == null){
            throw new InvalidOrder();
        }
        String trackUrl  = getTrackingUrl(order);
        Message message = new Message();
        reply.setMessage(message);

        if (trackUrl != null) {
            Tracking tracking = new Tracking();
            message.setTracking(tracking);
            tracking.setUrl(trackUrl);
            tracking.setStatus("active");
        }else {
            throw new TrackingNotSupported();
        }

    }

    public void cancel(Request request, Request reply){
        Order order = request.getMessage().getOrder();
        if (order == null){
            if (request.getMessage().getOrderId() != null){
                order = new Order();
                order.setId(request.getMessage().getOrderId());
            }
        }
        if (order == null){
            throw new InvalidOrder();
        }

        CancellationReasonCode code = request.getMessage().getEnum(CancellationReasonCode.class,"cancellation_reason_id",CancellationReasonCode.convertor);
        if (code != null && code.isUsableByBuyerParty()){
            Cancellation cancellation = order.getCancellation();
            if (cancellation == null){
                cancellation = new Cancellation();
                order.setCancellation(cancellation);
            }
            cancellation.setSelectedReason(new Option());
            cancellation.setCancelledBy(CancelledBy.BUYER);
            cancellation.setTime(request.getContext().getTimestamp());
            cancellation.getSelectedReason().setDescriptor(new Descriptor());
            cancellation.getSelectedReason().getDescriptor().setCode(CancellationReasonCode.convertor.toString(code));
        }else {
            throw new SellerException.InvalidCancellationReason();
        }

        Order cancelledOrder = cancel(order);
        Message message = new Message(); reply.setMessage(message);
        message.setOrder(cancelledOrder);
    }

    public void rating(Request request, Request reply) {
        getRatingCollector().rating(request,reply);
    }


    public void status(Request request, Request reply){
        Order order = request.getMessage().getOrder();
        if (order == null){
            if (request.getMessage().getOrderId() != null){
                order = new Order();
                order.setId(request.getMessage().getOrderId());
                request.getMessage().setOrder(order);
            }
        }
        if (order == null){
            throw new InvalidOrder();
        }
        reply.setMessage(new Message());
        Order current = getStatus(order);
        reply.getMessage().setOrder(current);
    }

    public void update(Request request, Request reply){
        throw new UpdationNotPossible("Orders cannot be updated. Please cancel and rebook your orders!");
    }
    public void issue(Request request,Request reply){
        IssueTracker tracker = getIssueTracker();
        tracker.save(request,reply);
    }
    public void issue_status(Request request,Request reply){
        IssueTracker tracker = getIssueTracker();
        tracker.status(request,reply);
    }
    public void receiver_recon(Request request ,Request reply){
        getReceiverReconProvider().receiver_recon(request,reply);
    }
}
