package in.succinct.bpp.core.adaptor;

import in.succinct.bpp.core.db.model.ProviderConfig.IssueTrackerConfig;

import java.lang.reflect.InvocationTargetException;
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

    private final Map<String,Class<IssueTracker>> cache = Collections.synchronizedMap(new HashMap<>());
    public void registerAdaptor(String name, Class<IssueTracker> adaptorClass){
        cache.put(name,adaptorClass);
    }


    public IssueTracker createIssueTracker(String name, IssueTrackerConfig config){
        Class<IssueTracker> trackerClass = cache.get(name);
        if (trackerClass == null){
            throw new RuntimeException("Not tracker registered as " + name);
        }
        try {
            return trackerClass.getConstructor(IssueTrackerConfig.class).newInstance(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
