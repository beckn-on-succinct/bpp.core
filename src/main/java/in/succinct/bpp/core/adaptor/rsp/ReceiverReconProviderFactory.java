package in.succinct.bpp.core.adaptor.rsp;

import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.rating.RatingCollector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReceiverReconProviderFactory {
    private static volatile ReceiverReconProviderFactory sSoleInstance;

    //private constructor.
    private ReceiverReconProviderFactory() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static ReceiverReconProviderFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (ReceiverReconProviderFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new ReceiverReconProviderFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected ReceiverReconProviderFactory readResolve() {
        return getInstance();
    }

    private final Map<String,Class<? extends ReceiverReconProvider>> cache = Collections.synchronizedMap(new HashMap<>());
    public <T extends ReceiverReconProvider> void registerAdaptor(String name, Class<T> adaptorClass){
        cache.put(name,adaptorClass);
    }


    public ReceiverReconProvider createReceiverReconProvider(CommerceAdaptor commerceAdaptor){
        Class<? extends ReceiverReconProvider> receiverReconProviderClass = cache.get(commerceAdaptor.getProviderConfig().getReceiverReconProviderConfig().getName());
        if (receiverReconProviderClass == null){
            throw new RuntimeException("Not Receiver Recon provider registered for " + commerceAdaptor.getProviderConfig().getReceiverReconProviderConfig().getName());
        }
        try {
            return receiverReconProviderClass.getConstructor(CommerceAdaptor.class).newInstance(commerceAdaptor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
