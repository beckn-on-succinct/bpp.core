package in.succinct.bpp.core.extensions;

import com.venky.extension.Extension;
import com.venky.extension.Registry;
import in.succinct.beckn.Order;
import in.succinct.bpp.core.db.model.BecknOrderMeta;


public class MessageArchiveCheck implements Extension {
    static {
        Registry.instance().registerExtension(BecknOrderMeta.class.getSimpleName() + ".archive.check",new MessageArchiveCheck());
    }
    @Override
    public void invoke(Object... context) {
        BecknOrderMeta message = (BecknOrderMeta) context[0];
        
        Order order = new Order(message.getOrderJson());
        
        if (!order.getStatus().isOpen()){
            message.setArchived(!order.getStatus().isPaymentRequired() || order.isPaid());
        }else {
            message.setArchived(false);
        }
        if (message.isArchived()){
            message.summarize(true);
        }
    }
}
