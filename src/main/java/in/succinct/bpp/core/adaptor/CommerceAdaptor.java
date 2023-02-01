package in.succinct.bpp.core.adaptor;

import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Items;
import in.succinct.beckn.Locations;
import in.succinct.beckn.Order;
import in.succinct.beckn.Payment;
import in.succinct.beckn.Payment.CollectedBy;
import in.succinct.beckn.Payment.PaymentType;
import in.succinct.beckn.Payments;
import in.succinct.beckn.Provider;
import in.succinct.beckn.Request;

import java.util.Map;

public abstract class CommerceAdaptor {
    private final Subscriber subscriber;
    private final Map<String,String> configuration;
    private final Application application ;

    public CommerceAdaptor(Map<String,String> configuration, Subscriber subscriber) {
        this.configuration = configuration;
        this.subscriber = subscriber;
        this.application = ApplicationUtil.find(getSubscriber().getAppId());;
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


    public abstract Locations getProviderLocations();
    public abstract Items getItems();
    public abstract boolean isTaxIncludedInPrice() ;
    public abstract Order initializeDraftOrder(Request request) ;
    public abstract Order confirmDraftOrder(Order draftOrder) ;
    public abstract Order getStatus(Order order);
    public abstract Order cancel(Order order) ;



    public abstract String getTrackingUrl(Order order) ;
    public abstract  String getSupportEmail() ;

    public String getProviderDescription(){
        return getConfiguration().getOrDefault(String.format("%s.provider.description",getSubscriber().getSubscriberId()),getSubscriber().getSubscriberId());
    }
    public String getProviderName(){
        return getConfiguration().getOrDefault(String.format("%s.provider.name",getSubscriber().getSubscriberId()),getSubscriber().getSubscriberId());
    }


    public Provider getProvider() {
        Provider provider = new Provider();
        provider.setDescriptor(new Descriptor());
        provider.getDescriptor().setName(getProviderName());
        provider.getDescriptor().setShortDesc(getProviderDescription());
        provider.getDescriptor().setLongDesc(getProviderDescription());
        provider.setId(getSubscriber().getSubscriberId()); // Provider is same as subscriber.!!
        provider.setTtl(120);
        provider.setPayments(getSupportedPaymentCollectionMethods());
        provider.setLocations(getProviderLocations());
        provider.setItems(getItems());
        return provider;
    }
    public boolean isCodSupported(){
        return false;
    }

    public Payments getSupportedPaymentCollectionMethods(){
        Payments payments = new Payments();
        Payment payment = new Payment();
        payment.setId("1");
        payment.setType(PaymentType.ON_ORDER);
        payment.setCollectedBy(CollectedBy.BAP);
        payments.add(payment);
        if (isCodSupported()){
            payment = new Payment();
            payment.setId("2");
            payment.setType(PaymentType.ON_FULFILLMENT);
            payment.setCollectedBy(CollectedBy.BPP);
            payments.add(payment);
        }
        return payments;
    }


}
