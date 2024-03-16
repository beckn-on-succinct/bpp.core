package in.succinct.bpp.core.adaptor.payments;

import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Registry;
import in.succinct.beckn.Subscriber;

public class PaymentLinkGeneratorFactory {
    private static volatile PaymentLinkGeneratorFactory sSoleInstance;

    //private constructor.
    private PaymentLinkGeneratorFactory() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static PaymentLinkGeneratorFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (PaymentLinkGeneratorFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new PaymentLinkGeneratorFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected PaymentLinkGeneratorFactory readResolve() {
        return getInstance();
    }

    public PaymentLinkGenerator createGenerator(Subscriber subscriber){
        ObjectHolder<PaymentLinkGenerator> o = new ObjectHolder<>(null);
        Registry.instance().callExtensions(PaymentLinkGenerator.class.getName(),subscriber, o);
        return o.get();
    }
}
