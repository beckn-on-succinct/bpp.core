package in.succinct.bpp.core.adaptor;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
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
import in.succinct.beckn.Order;
import in.succinct.beckn.Payment;
import in.succinct.beckn.Payment.CollectedBy;
import in.succinct.beckn.Payment.PaymentType;
import in.succinct.beckn.Payments;
import in.succinct.beckn.Provider;
import in.succinct.beckn.Request;
import in.succinct.beckn.Tag;
import in.succinct.beckn.Tag.List;
import in.succinct.bpp.core.adaptor.api.BecknIdHelper;
import in.succinct.bpp.core.adaptor.api.BecknIdHelper.Entity;
import in.succinct.bpp.core.db.model.BecknOrderMeta;
import in.succinct.bpp.core.db.model.ProviderConfig;
import in.succinct.bpp.core.db.model.ProviderConfig.DeliveryRules;
import org.json.simple.JSONArray;

import java.util.HashMap;
import java.util.Map;

public abstract class CommerceAdaptor {
    private final Subscriber subscriber;
    private final Map<String,String> configuration;
    private final Application application ;
    private final ProviderConfig providerConfig;

    public CommerceAdaptor(Map<String,String> configuration, Subscriber subscriber) {
        this.configuration = configuration;
        this.subscriber = subscriber;
        this.application = ApplicationUtil.find(getSubscriber().getAppId());
        String key = configuration.keySet().stream().filter(k->k.endsWith(".provider.config")).findAny().get();
        providerConfig = new ProviderConfig(this.configuration.get(key));
    }

    public ProviderConfig getProviderConfig() {
        return providerConfig;
    }


    public Application getApplication(){
        return application;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public Subscriber getSubscriber() {
        return subscriber;
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


        List serviceabilityTagDetail = new List();
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
        provider.getDescriptor().setCode(subscriber.getAppId());
        provider.getDescriptor().setShortDesc(config.getStoreName());
        provider.getDescriptor().setLongDesc(config.getStoreName());
        provider.getDescriptor().setImages(new Images());
        provider.getDescriptor().setSymbol(config.getLogo());
        provider.getDescriptor().getImages().add(config.getLogo());

        provider.setId(subscriber.getAppId()); // Provider is same as subscriber.!!
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
        provider.getCategories().add(providerConfig.getCategory());
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
                throw new RuntimeException("Don't know how to fulfil the order");
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
    public abstract Order confirmDraftOrder(Order draftOrder, BecknOrderMeta meta) ;
    public abstract Order getStatus(Order order);
    public abstract Order cancel(Order order) ;
    public abstract String getTrackingUrl(Order order) ;

}
