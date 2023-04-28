package in.succinct.bpp.core.db.model;

import com.venky.swf.db.Database;
import com.venky.swf.plugins.beckn.messaging.Subscriber;

public class LocalOrderSynchronizerFactory {
    private static volatile LocalOrderSynchronizerFactory sSoleInstance;

    //private constructor.
    private LocalOrderSynchronizerFactory() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static LocalOrderSynchronizerFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (LocalOrderSynchronizerFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new LocalOrderSynchronizerFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected LocalOrderSynchronizerFactory readResolve() {
        return getInstance();
    }

    public LocalOrderSynchronizer getLocalOrderSynchronizer(Subscriber subscriber){
        LocalOrderSynchronizer localOrderSynchronizer = Database.getInstance().getContext(LocalOrderSynchronizer.class.getName());
        if (localOrderSynchronizer == null) {
            localOrderSynchronizer = new LocalOrderSynchronizer(subscriber);
            Database.getInstance().setContext(LocalOrderSynchronizer.class.getName(), localOrderSynchronizer);
        }
        return localOrderSynchronizer;
    }
}
