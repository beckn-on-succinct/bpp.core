package in.succinct.bpp.core.adaptor;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.BecknObjects;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Fulfillment;
import in.succinct.beckn.Fulfillment.FulfillmentType;
import in.succinct.beckn.Fulfillments;
import in.succinct.beckn.Images;
import in.succinct.beckn.Items;
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
import in.succinct.bpp.core.db.model.ProviderConfig;
import in.succinct.bpp.core.db.model.ProviderConfig.DeliveryRules;

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
        cFulfillment.setId("1");
        cFulfillment.setContact(getProviderConfig().getSupportContact());
        return cFulfillment;
    }

    protected Fulfillment getHomeDelivery(){
        Fulfillment cFulfillment = new Fulfillment();
        cFulfillment.setType(FulfillmentType.home_delivery);
        cFulfillment.setId("2");
        cFulfillment.setContact(getProviderConfig().getSupportContact());
        cFulfillment.setProviderName(getProviderConfig().getFulfillmentProviderName());
        cFulfillment.setTracking(false);
        cFulfillment.setCategory(getProviderConfig().getFulfillmentCategory().toString());
        cFulfillment.setTAT(getProviderConfig().getFulfillmentTurnAroundTime());

        BecknObjects<BecknObject> list = new BecknObjects<>();
        cFulfillment.set("tags",list);

        List serviceabilityTags = new List();
        list.add(new Tag("serviceability",serviceabilityTags));

        DeliveryRules rules = getProviderConfig().getDeliveryRules();
        if (rules == null || rules.size() == 0) {
            serviceabilityTags.add(new Tag("location", getProviderConfig().getLocation().getId()));
            serviceabilityTags.add(new Tag("category", getProviderConfig().getCategory().getId()));
            serviceabilityTags.add(new Tag("type", "12"));
            serviceabilityTags.add(new Tag("val", "IND"));
            serviceabilityTags.add(new Tag("unit", "Country"));
        }else {
            serviceabilityTags.add(new Tag("location", getProviderConfig().getLocation().getId()));
            serviceabilityTags.add(new Tag("category", getProviderConfig().getCategory().getId()));
            serviceabilityTags.add(new Tag("type", "10"));
            serviceabilityTags.add(new Tag("val", rules.get(0).getMaxDistance()));
            serviceabilityTags.add(new Tag("unit", "km"));
        }
        return cFulfillment;

    }



    public Provider getProvider() {
        ProviderConfig config = getProviderConfig();
        Provider provider = new Provider();
        provider.setDescriptor(new Descriptor());
        provider.getDescriptor().setName(config.getStoreName());
        provider.getDescriptor().setCode(subscriber.getSubscriberId());
        provider.getDescriptor().setShortDesc(config.getStoreName());
        provider.getDescriptor().setLongDesc(config.getStoreName());
        provider.getDescriptor().setImages(new Images());
        provider.getDescriptor().setSymbol(config.getLogo());
        provider.getDescriptor().getImages().add(config.getLogo());

        provider.setId(getSubscriber().getSubscriberId()); // Provider is same as subscriber.!!
        provider.setTtl(120);
        if (!ObjectUtil.isVoid(config.getFssaiRegistrationNumber())) {
            provider.setFssaiLicenceNo(config.getFssaiRegistrationNumber());
        }
        provider.setPayments(getSupportedPaymentCollectionMethods());
        provider.setLocations(getProviderLocations());
        provider.setFulfillments(getFulfillments());
        provider.setItems(getItems());
        provider.setCategoryId(config.getCategory().getId());
        provider.setTime(config.getTime());
        return provider;
    }


    public Payments getSupportedPaymentCollectionMethods(){
        Payments payments = new Payments();
        Payment payment = new Payment();
        payment.setId("1");
        payment.setType(PaymentType.ON_ORDER);
        payment.setCollectedBy(CollectedBy.BAP);
        payments.add(payment);
        if (getProviderConfig().isCodSupported()){
            payment = new Payment();
            payment.setId("2");
            payment.setType(PaymentType.ON_FULFILLMENT);
            payment.setCollectedBy(CollectedBy.BPP);
            payments.add(payment);
        }
        return payments;
    }


    public abstract Locations getProviderLocations();
    public abstract Items getItems();
    public abstract boolean isTaxIncludedInPrice() ;
    public abstract Order initializeDraftOrder(Request request) ;
    public abstract Order confirmDraftOrder(Order draftOrder) ;
    public abstract Order getStatus(Order order);
    public abstract Order cancel(Order order) ;
    public abstract String getTrackingUrl(Order order) ;

}
