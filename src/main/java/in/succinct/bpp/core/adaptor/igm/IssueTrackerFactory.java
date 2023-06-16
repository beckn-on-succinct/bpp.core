package in.succinct.bpp.core.adaptor.igm;

import in.succinct.bpp.core.adaptor.CommerceAdaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IssueTrackerFactory {
    private static volatile IssueTrackerFactory sSoleInstance;

    //private constructor.
    private IssueTrackerFactory() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static IssueTrackerFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (IssueTrackerFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new IssueTrackerFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected IssueTrackerFactory readResolve() {
        return getInstance();
    }

    private final Map<String,Class<? extends IssueTracker>> cache = Collections.synchronizedMap(new HashMap<>());
    public <T extends IssueTracker> void registerAdaptor(String name, Class<T> adaptorClass){
        cache.put(name,adaptorClass);
    }


    public IssueTracker createIssueTracker(CommerceAdaptor adaptor){
        Class<? extends IssueTracker> trackerClass = cache.get(adaptor.getProviderConfig().getIssueTrackerConfig().getName());
        if (trackerClass == null){
            throw new RuntimeException("Not tracker registered as " + adaptor.getProviderConfig().getIssueTrackerConfig().getName());
        }
        try {
            return trackerClass.getConstructor(CommerceAdaptor.class).newInstance(adaptor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
