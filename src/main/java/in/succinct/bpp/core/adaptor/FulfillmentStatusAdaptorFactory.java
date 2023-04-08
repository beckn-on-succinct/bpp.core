package in.succinct.bpp.core.adaptor;

import com.venky.core.util.ObjectHolder;
import com.venky.extension.Registry;
import in.succinct.bpp.core.db.model.ProviderConfig;

public class FulfillmentStatusAdaptorFactory {
    private static volatile FulfillmentStatusAdaptorFactory sSoleInstance;

    //private constructor.
    private FulfillmentStatusAdaptorFactory() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static FulfillmentStatusAdaptorFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (FulfillmentStatusAdaptorFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new FulfillmentStatusAdaptorFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected FulfillmentStatusAdaptorFactory readResolve() {
        return getInstance();
    }

    public FulfillmentStatusAdaptor createAdaptor(CommerceAdaptor commerceAdaptor){
        try {
            ObjectHolder<FulfillmentStatusAdaptor> fulfillmentStatusAdaptorHolder = new ObjectHolder<>(null);
            Registry.instance().callExtensions(FulfillmentStatusAdaptor.class.getName(),commerceAdaptor,fulfillmentStatusAdaptorHolder);
            return fulfillmentStatusAdaptorHolder.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
