package in.succinct.bpp.core.adaptor;

import com.venky.core.util.ObjectHolder;
import com.venky.extension.Registry;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import in.succinct.bpp.core.registry.BecknRegistry;

import java.util.Map;

public class CommerceAdaptorFactory {
    private static volatile CommerceAdaptorFactory sSoleInstance;

    //private constructor.
    private CommerceAdaptorFactory() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static CommerceAdaptorFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (CommerceAdaptorFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new CommerceAdaptorFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected CommerceAdaptorFactory readResolve() {
        return getInstance();
    }

    public CommerceAdaptor createAdaptor(Map<String,String> properties, Subscriber subscriber, BecknRegistry registry){
        try {
            ObjectHolder<CommerceAdaptor> commerceAdaptorHolder = new ObjectHolder<>(null);
            Registry.instance().callExtensions(CommerceAdaptor.class.getName(),properties,subscriber,registry,commerceAdaptorHolder);
            return commerceAdaptorHolder.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
