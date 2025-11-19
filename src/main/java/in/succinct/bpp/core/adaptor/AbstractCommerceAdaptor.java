package in.succinct.bpp.core.adaptor;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import in.succinct.beckn.Catalog;
import in.succinct.beckn.Categories;
import in.succinct.beckn.Context;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Fulfillment;
import in.succinct.beckn.Fulfillment.FulfillmentStatus;
import in.succinct.beckn.Fulfillment.RetailFulfillmentType;
import in.succinct.beckn.Fulfillments;
import in.succinct.beckn.Images;
import in.succinct.beckn.Items;
import in.succinct.beckn.Location;
import in.succinct.beckn.Locations;
import in.succinct.beckn.Message;
import in.succinct.beckn.Order;
import in.succinct.beckn.Payment;
import in.succinct.beckn.Payment.CollectedBy;
import in.succinct.beckn.Payments;
import in.succinct.beckn.Provider;
import in.succinct.beckn.Provider.ServiceablityTags;
import in.succinct.beckn.Providers;
import in.succinct.beckn.Request;
import in.succinct.beckn.SellerException.InvalidOrder;
import in.succinct.beckn.SellerException.TrackingNotSupported;
import in.succinct.beckn.SellerException.UpdationNotPossible;
import in.succinct.beckn.Tracking;
import in.succinct.beckn.Tracking.Status;
import in.succinct.bpp.core.db.model.LocalOrderSynchronizerFactory;
import in.succinct.bpp.core.db.model.ProviderConfig;
import in.succinct.bpp.core.db.model.ProviderConfig.DeliveryRules;
import in.succinct.onet.core.api.BecknIdHelper;
import in.succinct.onet.core.api.BecknIdHelper.Entity;
import org.json.simple.JSONArray;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCommerceAdaptor extends CommerceAdaptor implements ItemFetcher{


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
        cFulfillment.setType(RetailFulfillmentType.store_pickup.toString());
        cFulfillment.setId(BecknIdHelper.getBecknId("1",getSubscriber(), Entity.fulfillment));
        cFulfillment.setContact(getProviderConfig().getSupportContact());
        cFulfillment.setFulfillmentStatus(FulfillmentStatus.Serviceable);
        return cFulfillment;
    }

    protected Fulfillment getHomeDelivery(){
        Fulfillment cFulfillment = new Fulfillment();
        cFulfillment.setType(RetailFulfillmentType.home_delivery.toString());
        cFulfillment.setId(BecknIdHelper.getBecknId("2",getSubscriber(), Entity.fulfillment));
        cFulfillment.setContact(getProviderConfig().getSupportContact());
        cFulfillment.setProviderName(getProviderConfig().getFulfillmentProviderName());
        cFulfillment.setTracking(false);
        cFulfillment.setCategory(getProviderConfig().getFulfillmentCategory().toString());
        cFulfillment.setTAT(getProviderConfig().getFulfillmentTurnAroundTime());


        cFulfillment.setFulfillmentStatus(FulfillmentStatus.Serviceable);
        return cFulfillment;

    }


    public Provider getProvider() {
        return getProvider(this);
    }

    public Provider getProvider(ItemFetcher fetcher){
        ProviderConfig config = getProviderConfig();
        Provider provider = new Provider();
        provider.setBppId(getSubscriber().getSubscriberId());
        provider.setDescriptor(new Descriptor());
        provider.getDescriptor().setName(config.getStoreName());
        provider.getDescriptor().setCode(getSubscriber().getSubscriberId());
        provider.getDescriptor().setShortDesc(config.getStoreName());
        provider.getDescriptor().setLongDesc(config.getStoreName());
        provider.getDescriptor().setImages(new Images());
        provider.getDescriptor().setSymbol(config.getLogo());
        provider.getDescriptor().getImages().add(config.getLogo());

        provider.setId(getSubscriber().getSubscriberId()); // Provider is same as subscriber.!!
        provider.setTtl(2L*60L*60L);
        if (!ObjectUtil.isVoid(config.getFssaiRegistrationNumber())) {
            provider.setFssaiLicenceNo(config.getFssaiRegistrationNumber());
        }
        provider.setPayments(getSupportedPaymentCollectionMethods());
        provider.setLocations(getProviderLocations());
        provider.setFulfillments(getFulfillments());
        provider.setItems(fetcher.getItems());
        provider.setCategoryId(config.getCategory().getId());
        provider.setCategories(new Categories());
        provider.getCategories().add(config.getCategory());
        provider.setServiceablityTags(new ServiceablityTags());

        ServiceablityTags serviceabilityTags = provider.getServiceablityTags();


        DeliveryRules rules = getProviderConfig().getDeliveryRules();
        if (rules == null || rules.isEmpty()) {
            serviceabilityTags.setTag("serviceability","location",getProviderConfig().getLocation().getId());
            serviceabilityTags.setTag("serviceability","category",getProviderConfig().getCategory().getId());
            serviceabilityTags.setTag("serviceability","type", "12");
            serviceabilityTags.setTag("serviceability","val", "IND");
            serviceabilityTags.setTag("serviceability","unit", "Country");
        }else {
            serviceabilityTags.setTag("serviceability","location",getProviderConfig().getLocation().getId());
            serviceabilityTags.setTag("serviceability","category",getProviderConfig().getCategory().getId());
            serviceabilityTags.setTag("serviceability","type", "10");
            serviceabilityTags.setTag("serviceability","val", String.valueOf(rules.get(0).getMaxDistance()));
            serviceabilityTags.setTag("serviceability","unit", "km");
        }
        //provider.setTime(config.getTime());
        return provider;
    }


    public Payments getSupportedPaymentCollectionMethods(){
        Payments payments = new Payments();
        Payment payment = new Payment();
        payment.setId(BecknIdHelper.getBecknId("1",getSubscriber(), Entity.payment));
        payment.setInvoiceEvent(FulfillmentStatus.Created);
        payment.setCollectedBy(CollectedBy.BAP);
        payments.add(payment);
        if (getProviderConfig().isCodSupported()){
            int i = 2;
            for (FulfillmentStatus invoiceEvent : new FulfillmentStatus[]{FulfillmentStatus.Created,FulfillmentStatus.Prepared, FulfillmentStatus.Completed}){
                payment = new Payment();
                payment.setId(BecknIdHelper.getBecknId(String.valueOf(i++),getSubscriber(), Entity.payment));
                payment.setInvoiceEvent(invoiceEvent);
                payment.setCollectedBy(CollectedBy.BPP);
                payments.add(payment);
            }
        }
        return payments;
    }
    public void fixFulfillment(Context context, Order order){
        LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(getSubscriber()).fixFulfillment(context,order);

        Map<String, in.succinct.beckn.Fulfillment> map = new HashMap<>();
        for (in.succinct.beckn.Fulfillment f :getFulfillments()) {
            map.put( f.getType(),f);
        }



        Fulfillment fulfillment = order.getFulfillment();

        if (fulfillment == null) {
            fulfillment = map.get(RetailFulfillmentType.store_pickup.toString());
            order.setFulfillment(fulfillment);
        }

        if (fulfillment != null && fulfillment.getType() == null){
            if (fulfillment._getEnd() == null && map.containsKey(RetailFulfillmentType.store_pickup.toString())) {
                fulfillment.setType(RetailFulfillmentType.store_pickup.toString());
            }else if (map.containsKey(RetailFulfillmentType.home_delivery.toString())){
                fulfillment.setType(RetailFulfillmentType.home_delivery.toString());
            }else {
                fulfillment = null;
                order.setFulfillment(null);
                order.getFulfillments().setInner(new JSONArray());
            }
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
        if (order._getProviderLocation() != null){
            if (locations.get(order._getProviderLocation().getId()) == null){
                locations.add(order._getProviderLocation());
            }
        }
        for (Location location : locations){
            Location actualLocation = allLocations.get(location.getId());
            location.rm("id");
            location.update(actualLocation);
        }
    }

    public abstract Locations getProviderLocations();
    public abstract Items getItems();
    public abstract boolean isTaxIncludedInPrice() ;
    public abstract Order confirmDraftOrder(Order draftOrder) ;
    public abstract Order getStatus(Order order);
    public abstract Order cancel(Order order) ;
    public abstract String getTrackingUrl(Order order) ;


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
        confirmedOrder.setUpdatedAt(request.getContext().getTimestamp());
        if (reply.getMessage() == null) {
            reply.setMessage(new Message());
        }
        reply.getMessage().setOrder(confirmedOrder);
    }


    public abstract void search(Request request, Request reply);
    public abstract void select(Request request, Request reply) ;


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
            tracking.setStatus(Status.active);
        }else {
            throw new TrackingNotSupported();
        }

    }

    public void cancel(Request request, Request reply){
        Order order = request.getMessage().getOrder();
        if (order == null){
            throw new InvalidOrder();
        }
        Order cancelledOrder = cancel(order);
        Message message = new Message(); reply.setMessage(message);
        message.setOrder(cancelledOrder);
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


    public String getCurrency(){
        return "INR";
    }

}
