package in.succinct.bpp.core.extensions;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.extension.Extension;
import com.venky.extension.Registry;
import com.venky.swf.path.Path;
import in.succinct.beckn.Context;
import in.succinct.beckn.Issue;
import in.succinct.beckn.Message;
import in.succinct.beckn.Request;
import in.succinct.beckn.Subscriber;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.NetworkAdaptor;
import in.succinct.bpp.core.db.model.LocalOrderSynchronizerFactory;

import java.util.Date;
import java.util.UUID;

public class IgmWebHook implements Extension {
    static {
        Registry.instance().registerExtension("in.succinct.bpp.shell.igm_hook",new IgmWebHook());
    }
    @Override
    public void invoke(Object... objects) {
        CommerceAdaptor adaptor = (CommerceAdaptor) objects[0];
        NetworkAdaptor networkAdaptor = (NetworkAdaptor) objects[1];
        Path path = (Path) objects[2];
        try {
            hook(adaptor, networkAdaptor, path);
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
    public void hook(CommerceAdaptor eCommerceAdaptor,NetworkAdaptor networkAdaptor, Path path) throws Exception{
        Issue issue = new Issue(StringUtil.read(path.getInputStream()));
        String transactionId = LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(eCommerceAdaptor.getSubscriber()).getTransactionId(issue.getOrder());


        final Request request = new Request();
        request.setMessage(new Message());
        request.setContext(new Context());
        request.getMessage().setIssue(issue);
        Context context = request.getContext();
        context.setTransactionId(transactionId);
        context.setBppId(eCommerceAdaptor.getSubscriber().getSubscriberId());
        context.setBppUri(eCommerceAdaptor.getSubscriber().getSubscriberUrl());
        Subscriber bap = null;
        if (ObjectUtil.equals(context.getBppId(),issue.getRespondent().getSubscriberId())){
            context.setBapId(issue.getComplainant().getSubscriberId());
        }else {
            context.setBapId(issue.getRespondent().getSubscriberId());
        }
        bap = networkAdaptor.lookup(context.getBapId(),true).get(0);
        context.setBapUri(bap.getSubscriberUrl());
        context.setCoreVersion("1.0.0");
        context.setCountry(eCommerceAdaptor.getSubscriber().getCountry());
        context.setCity(eCommerceAdaptor.getSubscriber().getCity());
        context.setTimestamp(new Date());
        if (issue.getComplainant().getSubscriberId().equals(eCommerceAdaptor.getSubscriber().getSubscriberId())){
            context.setAction("issue");
        }else {
            context.setAction("on_issue_status");
        }
        context.setDomain(eCommerceAdaptor.getSubscriber().getDomain());
        context.setNetworkId(networkAdaptor.getId());
        //Fill any other attributes needed.
        //Send unsolicited on_status.
        context.setMessageId(UUID.randomUUID().toString());

        networkAdaptor.getApiAdaptor().callback(eCommerceAdaptor,request);
    }
}
