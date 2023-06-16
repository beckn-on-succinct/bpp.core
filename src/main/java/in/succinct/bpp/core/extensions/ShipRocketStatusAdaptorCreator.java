package in.succinct.bpp.core.extensions;

import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.fulfillment.FulfillmentStatusAdaptor;

import java.util.HashMap;
import java.util.Map;

public class ShipRocketStatusAdaptorCreator implements Extension {
    static {
        Registry.instance().registerExtension(FulfillmentStatusAdaptor.class.getName(),new ShipRocketStatusAdaptorCreator());
    }
    @Override
    public void invoke(Object... context) {
        CommerceAdaptor commerceAdaptor = (CommerceAdaptor) context[0];
        if (commerceAdaptor.getProviderConfig().getLogisticsAppConfig() == null){
            return;
        }
        if (!ObjectUtil.equals(commerceAdaptor.getProviderConfig().getLogisticsAppConfig().get("name"),"ship_rocket")){
            return;
        }
        ObjectHolder<FulfillmentStatusAdaptor> holder = (ObjectHolder<FulfillmentStatusAdaptor>)context[1];

        ShipRocketStatusAdaptor shipRocketStatusAdaptor =  cache.get(commerceAdaptor.getSubscriber().getSubscriberId());
        if (shipRocketStatusAdaptor == null){
            synchronized (cache) {
                shipRocketStatusAdaptor = cache.get(commerceAdaptor.getSubscriber().getSubscriberId());
                if (shipRocketStatusAdaptor == null) {
                    shipRocketStatusAdaptor = new ShipRocketStatusAdaptor(commerceAdaptor);
                    cache.put(commerceAdaptor.getSubscriber().getSubscriberId(), shipRocketStatusAdaptor);
                }
            }
        }
        holder.set(shipRocketStatusAdaptor);
    }

    private static Map<String,ShipRocketStatusAdaptor> cache = new HashMap<>();
}
