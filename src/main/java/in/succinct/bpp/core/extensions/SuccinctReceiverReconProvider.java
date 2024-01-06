package in.succinct.bpp.core.extensions;

import com.venky.core.util.MultiException;
import in.succinct.beckn.Order;
import in.succinct.beckn.Order.Orders;
import in.succinct.beckn.Request;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.rsp.ReceiverReconProvider;
import in.succinct.bpp.core.adaptor.rsp.ReceiverReconProviderFactory;
import in.succinct.bpp.core.db.model.LocalOrderSynchronizer;
import in.succinct.bpp.core.db.model.LocalOrderSynchronizerFactory;

public class SuccinctReceiverReconProvider extends ReceiverReconProvider {

    static {
        ReceiverReconProviderFactory.getInstance().registerAdaptor("default",SuccinctReceiverReconProvider.class);
    }

    protected SuccinctReceiverReconProvider(CommerceAdaptor adaptor) {
        super(adaptor);
    }

    @Override
    public void receiver_recon(Request request, Request response) {
        response.setSuppressed(true); // will be sent later as a callback.
        Orders orders = request.getMessage().getOrders();
        LocalOrderSynchronizer synchronizer = LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(getAdaptor().getSubscriber());
        MultiException exception = new MultiException();
        for (Order order : orders){
            try {
                synchronizer.receiver_recon(request.getContext(),order, getAdaptor());
            }catch (RuntimeException e){
                exception.add(e);
            }
        }

    }
}
