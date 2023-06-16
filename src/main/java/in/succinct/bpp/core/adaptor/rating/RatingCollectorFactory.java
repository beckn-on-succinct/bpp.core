package in.succinct.bpp.core.adaptor.rating;

import in.succinct.bpp.core.adaptor.CommerceAdaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RatingCollectorFactory {
    private static volatile RatingCollectorFactory sSoleInstance;

    //private constructor.
    private RatingCollectorFactory() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static RatingCollectorFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (RatingCollectorFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new RatingCollectorFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected RatingCollectorFactory readResolve() {
        return getInstance();
    }
    private final Map<String,Class<? extends RatingCollector>> cache = Collections.synchronizedMap(new HashMap<>());
    public <T extends RatingCollector> void registerAdaptor(String name, Class<T> adaptorClass){
        cache.put(name,adaptorClass);
    }


    public RatingCollector createRatingCollector(CommerceAdaptor commerceAdaptor){
        Class<? extends RatingCollector> collectorClass = cache.get(commerceAdaptor.getProviderConfig().getRatingCollectorConfig().getName());
        if (collectorClass == null){
            throw new RuntimeException("Not rating collector registered as " + commerceAdaptor.getProviderConfig().getRatingCollectorConfig().getName());
        }
        try {
            return collectorClass.getConstructor(CommerceAdaptor.class).newInstance(commerceAdaptor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
