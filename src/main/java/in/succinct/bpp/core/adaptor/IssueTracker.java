package in.succinct.bpp.core.adaptor;

import in.succinct.beckn.Message;
import in.succinct.bpp.core.db.model.ProviderConfig.IssueTrackerConfig;

public abstract class IssueTracker {
    IssueTrackerConfig config;

    protected IssueTracker(IssueTrackerConfig config) {
        this.config = config;
    }

    public IssueTrackerConfig getConfig() {
        return config;
    }

    public abstract void save(Message message); //Create or update issue

    public abstract void status(Message message);
}
