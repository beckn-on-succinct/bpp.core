package in.succinct.bpp.core.db.model.igm.config;

import in.succinct.bpp.core.db.model.Subscriber;

import java.util.List;

public interface Application extends com.venky.swf.plugins.collab.db.model.participants.Application {

    List<Subscriber> getSubscribers();

}
