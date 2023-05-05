package in.succinct.bpp.core.adaptor;

import in.succinct.beckn.Message;
import in.succinct.beckn.Request;
import in.succinct.bpp.core.db.model.ProviderConfig.IssueTrackerConfig;

public abstract class IssueTracker {
    IssueTrackerConfig config;

    IssueTracker(IssueTrackerConfig config) {
        this.config = config;
    }

    public IssueTrackerConfig getConfig() {
        return config;
    }

    public abstract void save(Request request, Request response); //Create or update issue

    public abstract void status(Request request, Request response);
}
