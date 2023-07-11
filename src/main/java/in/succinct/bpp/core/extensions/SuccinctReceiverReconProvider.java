package in.succinct.bpp.core.extensions;

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
        for (Order order : orders){
            synchronizer.receiver_recon(order,getAdaptor().getProviderConfig());
        }

    }
}
