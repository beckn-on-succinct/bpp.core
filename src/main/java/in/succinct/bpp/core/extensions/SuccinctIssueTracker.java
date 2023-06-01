package in.succinct.bpp.core.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;
import in.succinct.beckn.Contact;
import in.succinct.beckn.Context;
import in.succinct.beckn.Issue.EscalationLevel;
import in.succinct.beckn.IssueCategory;
import in.succinct.beckn.IssueSubCategory;
import in.succinct.beckn.Odr.Odrs;
import in.succinct.beckn.Odr.PricingModel;
import in.succinct.beckn.Odr.Rating;
import in.succinct.beckn.Order;
import in.succinct.beckn.Organization;
import in.succinct.beckn.Person;
import in.succinct.beckn.Price;
import in.succinct.beckn.Representative.Complainant;
import in.succinct.beckn.Representative.Respondent;
import in.succinct.beckn.Representative.Role;
import in.succinct.beckn.Request;
import in.succinct.beckn.Resolution;
import in.succinct.beckn.Resolution.ResolutionAction;
import in.succinct.beckn.Resolution.ResolutionStatus;
import in.succinct.beckn.SelectedOdrs;
import in.succinct.beckn.SelectedOdrs.SelectedOdrsList;
import in.succinct.beckn.Status;
import in.succinct.beckn.Time;
import in.succinct.bpp.core.adaptor.IssueTracker;
import in.succinct.bpp.core.adaptor.IssueTrackerFactory;
import in.succinct.bpp.core.db.model.ProviderConfig.IssueTrackerConfig;
import in.succinct.bpp.core.db.model.igm.Issue;
import in.succinct.bpp.core.db.model.igm.Representative;
import in.succinct.bpp.core.db.model.igm.config.Application;
import in.succinct.bpp.core.db.model.igm.config.Odr;
import in.succinct.bpp.core.db.model.igm.config.PreferredOdr;
import in.succinct.bpp.core.db.model.igm.config.Subscriber;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class SuccinctIssueTracker extends IssueTracker {
    static {
        IssueTrackerFactory.getInstance().registerAdaptor("internal",SuccinctIssueTracker.class);
    }
    public SuccinctIssueTracker(IssueTrackerConfig config) {
        super(config);
    }

    @Override
    public void save(Request request, Request response) {
        Issue issue = getDbIssue(request.getContext(),request.getMessage().getIssue());
        in.succinct.beckn.Issue becknIssue = getBecknIssue(issue);
        response.getMessage().setIssue(becknIssue);
    }

    @Override
    public void status(Request request, Request response) {
        Issue issue = getDbIssue(request.getContext(),request.getMessage().getIssue());
        in.succinct.beckn.Issue becknIssue = getBecknIssue(issue);
        response.getMessage().setIssue(becknIssue);
    }

    public Issue getDbIssue(Context context, in.succinct.beckn.Issue issue){

        Issue dbIssue =  Database.getTable(Issue.class).newRecord();
        dbIssue.setIssueId(issue.getId());
        dbIssue = Database.getTable(Issue.class).getRefreshed(dbIssue);
        dbIssue.save();

        Representative complainant = getDbRepresentative(context,dbIssue, issue.getComplainant());
        dbIssue.setComplainantId(complainant.getId());

        dbIssue.setEscalationLevel(issue.getEscalationLevel().name());
        dbIssue.setExpectedResolutionTs(new Timestamp(issue.getCreatedAt().getTime() + issue.getExpectedResolutionTime().getDuration().getSeconds() * 1000 ));
        dbIssue.setExpectedResponseTs(new Timestamp(issue.getCreatedAt().getTime()  + issue.getExpectedResponseTime().getDuration().getSeconds() * 1000));
        Resolution resolution = issue.getResolution();
        if (resolution != null) {
            dbIssue.setDisputeResolutionRemarks(resolution.getDisputeResolutionRemarks());
            dbIssue.setGroResolutionRemarks(resolution.getGroResolutionRemarks());
            dbIssue.setResolution(resolution.getResolution());
            dbIssue.setResolutionRemarks(resolution.getResolutionRemarks());
            dbIssue.setResolutionAction(resolution.getResolutionAction().name());
            dbIssue.setResolutionStatus(resolution.getResolutionStatus().name());
        }
        dbIssue.setResolutionProviderId(getDbRepresentative(context,dbIssue,issue.getResolutionProvider()).getId());
        dbIssue.setIssueCategory(issue.getIssueCategory().name());
        dbIssue.setIssueSubCategory(issue.getIssueSubCategory().name());
        dbIssue.setRespondentId(getDbRepresentative(context,dbIssue,issue.getRespondent()).getId());
        dbIssue.setOrderJson(issue.getOrder().toString());
        dbIssue.setSatisfactorilyClosed(issue.isSatisfied());
        dbIssue.setStatus(issue.getStatus().n   ame());
        for (SelectedOdrs sordrs : issue.getSelectedOdrsList()) {
            in.succinct.beckn.Representative representative = sordrs.getRepresentative();
            Representative dbRepresentative = getDbRepresentative(context, dbIssue, representative);
            for (in.succinct.beckn.Odr odr : sordrs.getOdrs()) {
                Odr dbOdr = getDbOdr(context, dbIssue, odr);
            }

        }
        if (issue.getFinalizedOdr() != null) {
            Odr odr = getDbOdr(context, dbIssue, issue.getFinalizedOdr());
            dbIssue.setFinalizedOdrId(odr.getId());
        }
        dbIssue.save();
        return dbIssue;
    }

    private Odr getDbOdr(Context context, Issue issue,  in.succinct.beckn.Odr odr) {
        Odr dbOdr = Database.getTable(Odr.class).newRecord();
        dbOdr.setAboutInfo(odr.getAboutInfo());
        dbOdr.setCurrency(odr.getPricingModel().getPrice().getCurrency());
        dbOdr.setName(odr.getName());
        dbOdr.setPrice(odr.getPricingModel().getPrice().getValue());
        dbOdr.setPricingInfo(odr.getPricingModel().getPricingInfo());
        dbOdr.setRatingValue(odr.getRating().getRatingValue());
        dbOdr.setUrl(odr.getUrl());
        if (odr.getRepresentative() != null) {
            Representative representative = getDbRepresentative(context, issue, odr.getRepresentative());
            dbOdr.setSubscriberId(representative.getSubscriberId());// On the odr network
        }
        dbOdr = Database.getTable(Odr.class).getRefreshed(dbOdr);
        dbOdr.save();
        return dbOdr;
    }

    private Representative getDbRepresentative(Context context, Issue issue, in.succinct.beckn.Representative representative) {
        Company dbCompany = getOrganization(representative.getOrganization());
        if (ObjectUtil.isVoid(dbCompany.getDomainName())){
            dbCompany.setDomainName(representative.getSubscriberId());
        }
        dbCompany.save();

        Subscriber subscriber = Database.getTable(Subscriber.class).newRecord();
        subscriber.setSubscriberId(representative.getSubscriberId());

        Application application = Database.getTable(Application.class).newRecord();
        application.setAppId(representative.getSubscriberId());
        application.setCompanyId(dbCompany.getId());
        application.setIndustryClassificationCode(context.getDomain());
        application = Database.getTable(Application.class).getRefreshed(application);
        application.save();

        subscriber.setApplicationId(application.getId());
        subscriber.setDomainId(context.getDomain());
        subscriber.setNetworkId(context.getNetworkId());
        subscriber = Database.getTable(Subscriber.class).getRefreshed(subscriber);
        subscriber.save();

        Representative dbRepresentative = Database.getTable(Representative.class).newRecord();

        Contact contact =representative.getContact();
        Person person = representative.getPerson();
        if (contact != null || person != null){
            User user = Database.getTable(User.class).newRecord();
            if (contact != null) {
                user.setEmail(contact.getEmail());
                user.setPhoneNumber(contact.getPhone());
                user.setName(contact.getEmail());
            }
            if (person != null) {
                user.setName(person.getName());
            }
            user.setCompanyId(dbCompany.getId());
            user = Database.getTable(User.class).getRefreshed(user);
            user.save();
            dbRepresentative.setUserId(user.getId());
        }
        dbRepresentative.setIssueId(issue.getId());
        dbRepresentative.setSubscriberId(subscriber.getId());
        dbRepresentative.setRole(representative.getRole().name());
        dbRepresentative = Database.getTable(Representative.class).getRefreshed(dbRepresentative);
        dbRepresentative.save();

        return dbRepresentative;


    }


    private Company getOrganization(Organization organization) {
        Company dbCompany = Database.getTable(Company.class).newRecord();
        dbCompany.setName(organization.getName());
        if (organization.getDateOfIncorporation() != null) {
            dbCompany.setDateOfIncorporation(new Date(organization.getDateOfIncorporation().getTime()));
        }
        if (organization.getEmail() != null){
            String domain = organization.getEmail().substring(organization.getEmail().indexOf('@')+1);
            dbCompany.setDomainName(domain);
        }
        dbCompany = Database.getTable(Company.class).getRefreshed(dbCompany);
        return dbCompany;
    }

    public in.succinct.beckn.Issue getBecknIssue(Issue dbIssue){
        in.succinct.beckn.Issue issue = new in.succinct.beckn.Issue();
        issue.setComplainant(getBecknRepresentative(dbIssue.getComplainant(), Complainant.class));

        issue.setEscalationLevel(EscalationLevel.valueOf(dbIssue.getEscalationLevel()));
        issue.setExpectedResolutionTime(new Time());
        issue.getExpectedResolutionTime().setDuration(Duration.of(dbIssue.getExpectedResolutionTs().getTime() - dbIssue.getCreatedAt().getTime(), ChronoUnit.MILLIS));

        issue.setExpectedResponseTime(new Time());
        issue.getExpectedResponseTime().setDuration(Duration.of(dbIssue.getExpectedResponseTs().getTime() - dbIssue.getCreatedAt().getTime(), ChronoUnit.MILLIS));

        Resolution resolution = new Resolution();
        issue.setResolution(resolution);

        resolution.setDisputeResolutionRemarks(dbIssue.getDisputeResolutionRemarks());
        resolution.setGroResolutionRemarks(dbIssue.getGroResolutionRemarks());
        resolution.setResolution(dbIssue.getResolution());
        resolution.setResolutionRemarks(dbIssue.getResolutionRemarks());
        resolution.setResolutionAction(ResolutionAction.valueOf(dbIssue.getResolutionAction()));
        resolution.setResolutionStatus(ResolutionStatus.valueOf(dbIssue.getResolutionStatus()));

        issue.setResolutionProvider(getBecknRepresentative(dbIssue.getResolutionProvider(), in.succinct.beckn.Representative.class));

        issue.setIssueCategory(IssueCategory.valueOf(dbIssue.getIssueCategory()));
        issue.setIssueSubCategory(IssueSubCategory.valueOf(dbIssue.getIssueSubCategory()));
        issue.setRespondent(getBecknRepresentative(dbIssue.getRespondent(), Respondent.class));
        issue.setOrder(new Order(dbIssue.getOrderJson()));
        issue.setSatisfied(dbIssue.isSatisfactorilyClosed());
        issue.setStatus(Status.valueOf(dbIssue.getStatus()));
        issue.setSelectedOdrsList(new SelectedOdrsList());

        dbIssue.getRepresentors().forEach(dbRepresentative -> {
            SelectedOdrs selectedOdrs = new SelectedOdrs();
            selectedOdrs.setRepresentative(getBecknRepresentative(dbRepresentative, in.succinct.beckn.Representative.class));
            selectedOdrs.setOdrs(new Odrs());
            for (PreferredOdr preferredOdr : dbRepresentative.getSubscriber().getPreferredOdrs()) {
                Odr dbOdr = preferredOdr.getOdr();
                in.succinct.beckn.Odr becknOdr = getBecknOdr(dbOdr);
                selectedOdrs.getOdrs().add(becknOdr);
            }
        });
        in.succinct.beckn.Odr finalizedOdr = getBecknOdr(dbIssue.getFinalizedOdr());
        issue.setFinalizedOdr(finalizedOdr);
        return issue;
    }

    private in.succinct.beckn.Odr getBecknOdr(Odr dbOdr) {
        in.succinct.beckn.Odr becknOdr = new in.succinct.beckn.Odr();
        becknOdr.setAboutInfo(dbOdr.getAboutInfo());
        becknOdr.setName(dbOdr.getName());
        becknOdr.setPricingModel(new PricingModel());
        becknOdr.getPricingModel().setPrice(new Price());
        becknOdr.getPricingModel().getPrice().setValue(dbOdr.getPrice());
        becknOdr.getPricingModel().getPrice().setCurrency(dbOdr.getCurrency());
        becknOdr.getPricingModel().setPricingInfo(dbOdr.getPricingInfo());
        becknOdr.setRating(new Rating());
        becknOdr.getRating().setRatingValue(dbOdr.getRatingValue());
        becknOdr.setUrl(dbOdr.getUrl());
        becknOdr.setRepresentative(getBecknRepresentative(dbOdr.getSubscriber(), in.succinct.beckn.Representative.class));

        return becknOdr;
    }

    private <T extends in.succinct.beckn.Representative> T getBecknRepresentative(Subscriber subscriber, Class<T> representativeClass) {
        T representative = null;
        try {
            representative = representativeClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        representative.setRole(Role.ODR_ARBITRATOR);
        representative.setSubscriberId(subscriber.getSubscriberId());
        representative.setOrganization(getBecknOrganization(subscriber));
        return representative;
    }

    private <T extends in.succinct.beckn.Representative> T getBecknRepresentative(Representative dbRepresentative, Class<T> clazz)  {

        T representative = null;
        try {
            representative = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        representative.setRole(Role.valueOf(dbRepresentative.getRole()));
        Organization organization = getBecknOrganization(dbRepresentative.getSubscriber());
        representative.setContact(getBecknContact(dbRepresentative.getUser()));
        representative.setPerson(getBecknPerson(dbRepresentative.getUser()));
        representative.setSubscriberId(dbRepresentative.getSubscriber().getSubscriberId());
        return representative;
    }

    private Person getBecknPerson(User user) {
        Person person = new Person();
        person.setName(user.getName());
        return person;
    }

    private Contact getBecknContact(User user) {
        Contact contact = new Contact();
        contact.setEmail(user.getEmail());
        contact.setPhone(user.getPhoneNumber());
        return contact;
    }

    private Organization getBecknOrganization(Subscriber subscriber) {
        Company company = subscriber.getApplication().getCompany();
        Organization organization = new Organization();
        organization.setName(company.getName());
        organization.setDateOfIncorporation(company.getDateOfIncorporation());
        return organization;
    }


}
