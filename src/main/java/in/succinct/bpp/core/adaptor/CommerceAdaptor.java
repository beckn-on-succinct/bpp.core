package in.succinct.bpp.core.adaptor;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.db.model.application.ApplicationPublicKey;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.db.model.application.api.EndPoint;
import com.venky.swf.plugins.beckn.messaging.Subscriber;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.util.CompanyFinder;
import in.succinct.beckn.CancellationReasons;
import in.succinct.beckn.CancellationReasons.CancellationReasonCode;
import in.succinct.beckn.Contact;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Message;
import in.succinct.beckn.Option;
import in.succinct.beckn.Organization;
import in.succinct.beckn.RatingCategories;
import in.succinct.beckn.Request;
import in.succinct.beckn.ReturnReasons;
import in.succinct.beckn.ReturnReasons.ReturnReasonCode;
import in.succinct.beckn.User;
import in.succinct.bpp.core.adaptor.fulfillment.FulfillmentStatusAdaptor;
import in.succinct.bpp.core.adaptor.fulfillment.FulfillmentStatusAdaptorFactory;
import in.succinct.bpp.core.adaptor.igm.IssueTracker;
import in.succinct.bpp.core.adaptor.igm.IssueTrackerFactory;
import in.succinct.bpp.core.adaptor.rating.RatingCollector;
import in.succinct.bpp.core.adaptor.rating.RatingCollectorFactory;
import in.succinct.bpp.core.db.model.ProviderConfig;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;

public abstract class CommerceAdaptor{
    private final Subscriber subscriber;
    private final Map<String,String> configuration;
    private final Application application ;
    private final ProviderConfig providerConfig;
    private final FulfillmentStatusAdaptor fulfillmentStatusAdaptor ;
    private final IssueTracker issueTracker;
    private final RatingCollector ratingCollector;

    public CommerceAdaptor(Map<String,String> configuration, Subscriber subscriber) {
        this.configuration = configuration;
        this.subscriber = subscriber;
        String key = configuration.keySet().stream().filter(k->k.endsWith(".provider.config")).findAny().get();
        this.providerConfig = new ProviderConfig(this.configuration.get(key));
        this.subscriber.setOrganization(providerConfig.getOrganization());
        this.fulfillmentStatusAdaptor = FulfillmentStatusAdaptorFactory.getInstance().createAdaptor(this);
        this.issueTracker = providerConfig.getIssueTrackerConfig() == null ? null : IssueTrackerFactory.getInstance().createIssueTracker(this);
        this.ratingCollector = providerConfig.getRatingCollectorConfig() == null ? null : RatingCollectorFactory.getInstance().createRatingCollector(this);
        this.application = getApplication(getSubscriber().getAppId());
    }
    public Application getApplication(String appId){
        Application app = ApplicationUtil.find(appId);
        Application application  = app;
        if (application == null) {
            application = Database.getTable(Application.class).newRecord();
            application.setAppId(subscriber.getAppId());
            application.setHeaders("(created) (expires) digest");
            application.setSignatureLifeMillis(5000);
            application.setSigningAlgorithm(Request.SIGNATURE_ALGO);
            application.setHashingAlgorithm("BLAKE2B-512");
            application = Database.getTable(Application.class).getRefreshed(application);
            application.save();
        }


        //CryptoKey cryptoKey = CryptoKey.find(subscriber.getCryptoKeyId(), CryptoKey.PURPOSE_SIGNING);

        ApplicationPublicKey publicKey = Database.getTable(ApplicationPublicKey.class).newRecord();
        publicKey.setApplicationId(application.getId());
        publicKey.setPurpose(CryptoKey.PURPOSE_SIGNING);
        publicKey.setAlgorithm(Request.SIGNATURE_ALGO);
        publicKey.setKeyId(subscriber.getUniqueKeyId());
        publicKey.setValidFrom(new Timestamp(subscriber.getValidFrom().getTime()));
        publicKey.setValidUntil(new Timestamp(subscriber.getValidTo().getTime()));
        publicKey.setPublicKey(Request.getPemSigningKey(subscriber.getSigningPublicKey()));
        publicKey = Database.getTable(ApplicationPublicKey.class).getRefreshed(publicKey);
        publicKey.save();

        EndPoint endPoint = Database.getTable(EndPoint.class).newRecord();
        endPoint.setApplicationId(application.getId());
        endPoint.setBaseUrl(subscriber.getSubscriberUrl());
        endPoint = Database.getTable(EndPoint.class).getRefreshed(endPoint);
        endPoint.save();

        return app;
    }

    public IssueTracker getIssueTracker() {
        return issueTracker;
    }

    public RatingCollector getRatingCollector() {
        return ratingCollector;
    }

    public FulfillmentStatusAdaptor getFulfillmentStatusAdaptor() {
        return fulfillmentStatusAdaptor;
    }

    public ProviderConfig getProviderConfig() {
        return providerConfig;
    }


    public Application getApplication(){
        return application;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public abstract void search(Request request, Request response);
    public abstract void select(Request request, Request response);
    public abstract void init(Request request, Request response);
    public abstract void confirm(Request request, Request response);
    public abstract void track(Request request, Request response);
    public abstract void cancel(Request request, Request response);
    public abstract void update(Request request, Request response);
    public abstract void status(Request request, Request response);
    public void rating(Request request, Request reply) {
        getRatingCollector().rating(request,reply);
    }

    public void support(Request request, Request reply) {
        reply.setMessage(new Message());
        reply.getMessage().setEmail(getProviderConfig().getSupportContact().getEmail());
        reply.getMessage().setPhone(getProviderConfig().getSupportContact().getPhone());
    }


    public void get_cancellation_reasons(Request request, Request reply) {
        reply.setCancellationReasons(new CancellationReasons());
        for (CancellationReasonCode cancellationReasonCode : CancellationReasonCode.values()) {
            Option option = new Option();
            option.setDescriptor(new Descriptor());
            option.getDescriptor().setCode(CancellationReasonCode.convertor.toString(cancellationReasonCode));
            reply.getCancellationReasons().add(option);
        }
    }

    public void get_return_reasons(Request request, Request reply) {
        reply.setReturnReasons(new ReturnReasons());
        for (ReturnReasonCode returnReasonCode : ReturnReasonCode.values()) {
            Option option = new Option();
            option.setDescriptor(new Descriptor());
            option.getDescriptor().setCode(ReturnReasonCode.convertor.toString(returnReasonCode));
            reply.getReturnReasons().add(option);
        }
    }

    public void get_rating_categories(Request request, Request reply) {
        reply.setRatingCategories(new RatingCategories());
    }


    public void get_feedback_form(Request request, Request reply) {
    }


    public void _search(Request reply){

    }
    public void clearCache() {

    }

    public Company createCompany(Organization organization, String subscriberId) {
        User user = organization != null ? organization.getAuthorizedSignatory() : null ;
        Contact contact = user == null ? null : user.getContact();
        String email = contact == null ? null : contact.getEmail();
        Company company = null;
        Company companyByEmail = null;
        if (email != null){
            String domain = email.substring(email.indexOf('@')+1);
            companyByEmail = CompanyFinder.getInstance().find(domain);
        }
        Company companyBySubscriberId = CompanyFinder.getInstance().find(subscriberId);
        Company companyByName = null;
        if (organization != null && !ObjectUtil.isVoid(organization.getName())) {
            companyByName = Database.getTable(Company.class).newRecord();
            companyByName.setName(organization.getName());
            if (organization.getDateOfIncorporation() != null) {
                companyByName.setDateOfIncorporation(new Date(organization.getDateOfIncorporation().getTime()));
            }
            companyByName = Database.getTable(Company.class).getRefreshed(companyByName);
        }

        if (companyByEmail != null && !companyByEmail.getRawRecord().isNewRecord()){
            company = companyByEmail;
        }else if (companyBySubscriberId != null && !companyBySubscriberId.getRawRecord().isNewRecord()){
            company = companyBySubscriberId;
        }else if (companyByName != null && !companyByName.getRawRecord().isNewRecord()){
            company  = companyByName;
        }else {
            company = Database.getTable(Company.class).newRecord();
        }

        if (ObjectUtil.isVoid(company.getName())) {
            if (companyByName != null) {
                company.setName(companyByName.getName());
            }
        }
        if (ObjectUtil.isVoid(company.getDomainName())) {
            if (companyByEmail != null) {
                company.setDomainName(companyByEmail.getDomainName());
            }else if (companyBySubscriberId != null) {
                company.setDomainName(companyBySubscriberId.getDomainName());
            }
        }
        if (ObjectUtil.isVoid(company.getName())) {
            company.setName(company.getDomainName());
        }
        company.save();
        return company;
    }

    public Application createApplication(Company company, String subscriberId , String nicCode) {
        com.venky.swf.plugins.collab.db.model.participants.Application application = Database.getTable(com.venky.swf.plugins.collab.db.model.participants.Application.class).newRecord();
        if (ObjectUtil.equals(subscriberId,subscriber.getSubscriberId())) {
            application.setAppId(subscriber.getAppId());
        }
        if (ObjectUtil.isVoid(application.getAppId())){
            application.setAppId(subscriberId);
        }
        application = Database.getTable(com.venky.swf.plugins.collab.db.model.participants.Application.class).getRefreshed(application);

        application.setCompanyId(company.getId());
        if (!ObjectUtil.isVoid(nicCode)) {
            application.setIndustryClassificationCode(nicCode);
        }
        application.setCompanyId(company.getId());
        application.setHeaders("(created) (expires) digest");
        application.setSignatureLifeMillis(5000);
        application.setSigningAlgorithm(Request.SIGNATURE_ALGO);
        application.setHashingAlgorithm("BLAKE2B-512");
        //application.setSigningAlgorithmCommonName(application.getSigningAlgorithm().toLowerCase());
        //application.setHashingAlgorithmCommonName(application.getHashingAlgorithm().toLowerCase());
        application.save();
        return application;
    }


    public void issue(Request request,Request reply){
        IssueTracker tracker = getIssueTracker();
        tracker.save(request,reply);
    }
    public void issue_status(Request request,Request reply){
        IssueTracker tracker = getIssueTracker();
        tracker.status(request,reply);
    }

}
