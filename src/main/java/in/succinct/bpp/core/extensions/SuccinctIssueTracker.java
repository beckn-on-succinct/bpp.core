package in.succinct.bpp.core.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.application.Application;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.plugins.security.db.model.UserRole;
import in.succinct.beckn.Contact;
import in.succinct.beckn.Context;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Image;
import in.succinct.beckn.Images;
import in.succinct.beckn.Issue.EscalationLevel;
import in.succinct.beckn.Issue.Status;
import in.succinct.beckn.IssueCategory;
import in.succinct.beckn.IssueSubCategory;
import in.succinct.beckn.Message;
import in.succinct.beckn.Note;
import in.succinct.beckn.Note.Action;
import in.succinct.beckn.Note.Notes;
import in.succinct.beckn.Note.RepresentativeAction;
import in.succinct.beckn.Odr.Odrs;
import in.succinct.beckn.Odr.PricingModel;
import in.succinct.beckn.Odr.Rating;
import in.succinct.beckn.Order;
import in.succinct.beckn.Organization;
import in.succinct.beckn.Person;
import in.succinct.beckn.Price;
import in.succinct.beckn.Representative.Complainant;
import in.succinct.beckn.Representative.Representatives;
import in.succinct.beckn.Representative.Respondent;
import in.succinct.beckn.Request;
import in.succinct.beckn.Resolution;
import in.succinct.beckn.Resolution.ResolutionAction;
import in.succinct.beckn.Resolution.ResolutionStatus;
import in.succinct.beckn.Role;
import in.succinct.beckn.SelectedOdrs;
import in.succinct.beckn.SelectedOdrs.SelectedOdrsList;
import in.succinct.beckn.Time;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.igm.IssueTracker;
import in.succinct.bpp.core.adaptor.igm.IssueTrackerFactory;
import in.succinct.bpp.core.db.model.ProviderConfig;
import in.succinct.bpp.core.db.model.Subscriber;
import in.succinct.bpp.core.db.model.igm.Issue;
import in.succinct.bpp.core.db.model.igm.NoteAttachment;
import in.succinct.bpp.core.db.model.igm.Representative;
import in.succinct.bpp.core.db.model.igm.config.Odr;
import in.succinct.bpp.core.db.model.igm.config.PossibleRepresentative;
import in.succinct.bpp.core.db.model.igm.config.PreferredOdr;
import in.succinct.bpp.core.db.model.rsp.BankAccount;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SuccinctIssueTracker extends IssueTracker {
    static {
        IssueTrackerFactory.getInstance().registerAdaptor("default", SuccinctIssueTracker.class);
    }

    public SuccinctIssueTracker(CommerceAdaptor adaptor) {
        super(adaptor);
    }

    @Override
    public void save(Request request, Request response) {
        Issue issue = getDbIssue(request.getContext(), request.getMessage().getIssue());
        in.succinct.beckn.Issue becknIssue = getBecknIssue(issue);
        if (response.getMessage() == null) {
            response.setMessage(new Message());
        }
        response.getMessage().setIssue(becknIssue);
    }

    @Override
    public void status(Request request, Request response) {
        Issue dbIssue = Database.getTable(Issue.class).newRecord();
        dbIssue.setIssueId(request.getMessage().getIssue().getId());
        dbIssue = Database.getTable(Issue.class).getRefreshed(dbIssue);
        if (dbIssue.getRawRecord().isNewRecord()) {
            throw new RuntimeException("Invalid Issue");
        }

        in.succinct.beckn.Issue becknIssue = getBecknIssue(dbIssue);
        if (response.getMessage() == null) {
            response.setMessage(new Message());
        }
        response.getMessage().setIssue(becknIssue);
    }

    public Issue getDbIssue(Context context, in.succinct.beckn.Issue issue) {

        Issue dbIssue = Database.getTable(Issue.class).newRecord();
        dbIssue.setIssueId(issue.getId());
        dbIssue = Database.getTable(Issue.class).getRefreshed(dbIssue);
        if (dbIssue.getRawRecord().isNewRecord()) {
            if (issue.getCreatedAt() != null) {
                dbIssue.setCreatedAt(new Timestamp(issue.getCreatedAt().getTime()));
            }
            dbIssue.save();
            updateSelfInfo(context, issue, dbIssue);
        } else {
            in.succinct.beckn.Issue persisted = getBecknIssue(dbIssue);
            persisted.setOrder(null);
            issue.update(persisted, false);
        }


        Representative complainant = getDbRepresentative(context, dbIssue, issue.getComplainant());
        dbIssue.setComplainantId(complainant.getId());
        if (issue.getEscalationLevel() != null) {
            dbIssue.setEscalationLevel(issue.getEscalationLevel().name());
        }
        dbIssue.setExpectedResolutionTs(new Timestamp(issue.getCreatedAt().getTime() + issue.getExpectedResolutionTime().getDuration().getSeconds() * 1000));
        dbIssue.setExpectedResponseTs(new Timestamp(issue.getCreatedAt().getTime() + issue.getExpectedResponseTime().getDuration().getSeconds() * 1000));
        Resolution resolution = issue.getResolution();
        if (resolution != null) {
            dbIssue.setDisputeResolutionRemarks(resolution.getDisputeResolutionRemarks());
            dbIssue.setGroResolutionRemarks(resolution.getGroResolutionRemarks());
            dbIssue.setResolution(resolution.getResolution());
            dbIssue.setResolutionRemarks(resolution.getResolutionRemarks());
            dbIssue.setResolutionAction(resolution.getResolutionAction().name());
            dbIssue.setResolutionStatus(resolution.getResolutionStatus().name());
        }
        dbIssue.setResolutionProviderId(getDbRepresentative(context, dbIssue, issue.getResolutionProvider()).getId());
        dbIssue.setIssueCategory(issue.getIssueCategory().name());
        dbIssue.setIssueSubCategory(issue.getIssueSubCategory().name());

        Representative respondent = null;
        if (issue.getRespondent() != null){
            respondent = getDbRepresentative(context, dbIssue, issue.getRespondent());
            dbIssue.setRespondentId(respondent.getId());
        }
        if (issue.getOrder() != null) {
            dbIssue.setOrderJson(issue.getOrder().toString());
        }
        dbIssue.setSatisfactorilyClosed(issue.isSatisfied());
        dbIssue.setStatus(issue.getStatus().name());
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
        if (issue.getNotes() == null) {
            issue.setNotes(new Notes());
        }
        for (Note note : issue.getNotes()) {
            in.succinct.bpp.core.db.model.igm.Note dbNote = in.succinct.bpp.core.db.model.igm.Note.find(note.getId());
            if (dbNote == null) {
                dbNote = Database.getTable(in.succinct.bpp.core.db.model.igm.Note.class).newRecord();
                dbNote.setNoteId(note.getId());
                dbNote.setAction(note.getAction().getComplainantAction().name());
                dbNote.setIssueId(dbIssue.getId());
                dbNote.setNotes(note.getDescription().getLongDesc());
                dbNote.setSummary(note.getDescription().getShortDesc());

                Representative current = ObjectUtil.equals(issue.getComplainant().getSubscriberId(), note.getCreatedBy().getSubscriberId()) ? complainant :
                        (issue.getRespondent() != null && ObjectUtil.equals(issue.getRespondent().getSubscriberId(), note.getCreatedBy().getSubscriberId()) ? respondent :
                                getDbRepresentative(context, dbIssue, note.getCreatedBy()));

                dbNote.setLoggedByRepresentorId(current.getId());
                dbNote.setCreatedAt(new Timestamp(note.getCreatedAt().getTime()));

                if (!ObjectUtil.isVoid(note.getParentNoteId())) {
                    in.succinct.bpp.core.db.model.igm.Note parentNote = in.succinct.bpp.core.db.model.igm.Note.find(note.getParentNoteId());
                    if (parentNote != null) {
                        dbNote.setParentNoteId(parentNote.getId());
                    }
                }
                dbNote.save();
                if (note.getDescription().getImages() != null) {
                    for (Image image : note.getDescription().getImages()) {
                        NoteAttachment attachment = Database.getTable(NoteAttachment.class).newRecord();
                        attachment.setUploadUrl(image.getUrl());
                        attachment.setNoteId(dbNote.getId());
                        attachment.save();
                    }
                }

            }/*else no modification needed */

        }
        dbIssue.save();
        return dbIssue;
    }

    private void updateSelfInfo(Context context, in.succinct.beckn.Issue issue, Issue dbIssue) {
        Complainant complainant = issue.getComplainant();
        Respondent respondent = issue.getRespondent();
        in.succinct.beckn.Representative selfRepresentative = null;
        if (complainant == null) {
            complainant = new Complainant();
            complainant.setRole(Role.COMPLAINANT_PLATFORM);
            issue.setComplainant(complainant);
            selfRepresentative = complainant;
        }else if (ObjectUtil.equals(complainant.getSubscriberId(), getAdaptor().getSubscriber().getSubscriberId())) {
            selfRepresentative = complainant;
        }else if (respondent == null) {
            respondent = new Respondent();
            respondent.setRole(Role.RESPONDENT_PLATFORM);
            issue.setRespondent(respondent);
            selfRepresentative = respondent;
        }else if (ObjectUtil.equals(respondent.getSubscriberId(), getAdaptor().getSubscriber().getSubscriberId())) {
            selfRepresentative = respondent;
        }

        issue.setResolutionProvider(respondent);


        ProviderConfig providerConfig = getAdaptor().getProviderConfig();

        Organization selfOrg = providerConfig.getOrganization();
        Contact supportContact = providerConfig.getSupportContact();
        CommerceAdaptor adaptor = getAdaptor();

        Company company = adaptor.createCompany(selfOrg,adaptor.getSubscriber().getSubscriberId());
        com.venky.swf.db.model.application.Application application = getAdaptor().getApplication();


        selfRepresentative.setSubscriberId(adaptor.getSubscriber().getSubscriberId());
        selfRepresentative.setOrganization(selfOrg);
        selfRepresentative.setContact(providerConfig.getSupportContact());
        selfRepresentative.setPerson(selfOrg.getAuthorizedSignatory().getPerson());
        selfRepresentative.setFaqUrl(selfOrg.getFaqUrl());

        BankAccount bankAccount = Database.getTable(BankAccount.class).newRecord();
        bankAccount.setName(providerConfig.getOrganization().getName());
        bankAccount.setVirtualPaymentAddress(providerConfig.getSettlementDetail().getUpiAddress());
        bankAccount = Database.getTable(BankAccount.class).getRefreshed(bankAccount);
        bankAccount.save();


        Subscriber subscriber = Database.getTable(Subscriber.class).newRecord();
        subscriber.setSubscriberId(adaptor.getSubscriber().getSubscriberId());
        subscriber.setNetworkId(context.getNetworkId());

        subscriber = Database.getTable(Subscriber.class).getRefreshed(subscriber);
        subscriber.setApplicationId(application.getId());
        subscriber.setBankAccountId(bankAccount.getId());
        subscriber.setDomainId(adaptor.getSubscriber().getDomain());
        subscriber.setRole(adaptor.getSubscriber().getType());
        subscriber.save();

        if (issue.getRepresentatives() == null) {
            issue.setRepresentatives(new Representatives());
        }
        boolean subscriberRepresentativeAdded = false;
        for (in.succinct.beckn.Representative issueRepresentative : issue.getRepresentatives()) {
            if (ObjectUtil.equals(issueRepresentative.getSubscriberId(), subscriber.getSubscriberId())) {
                subscriberRepresentativeAdded = true;
                break;
            }
        }

        if (!subscriberRepresentativeAdded) {
            String baseRole = selfRepresentative.getRole().name().substring(0, selfRepresentative.getRole().name().lastIndexOf("_"));
            Set<String> rolesPendingToBeAdded = new HashSet<>();
            for (String r : new String[]{"PLATFORM", "GRO", "ODR"}) {
                rolesPendingToBeAdded.add(String.format("%s_%s", baseRole, r));
            }

            for (Iterator<PossibleRepresentative> iterator = subscriber.getPossibleRepresentatives().iterator(); iterator.hasNext() && !rolesPendingToBeAdded.isEmpty(); ) {
                PossibleRepresentative possibleRepresentative = iterator.next();
                User user = possibleRepresentative.getUser();
                Representative dbRepresentative = Database.getTable(Representative.class).newRecord();
                in.succinct.beckn.Representative aRepresentative = new in.succinct.beckn.Representative();

                dbRepresentative.setIssueId(dbIssue.getId());

                aRepresentative.setSubscriberId(selfRepresentative.getSubscriberId());
                dbRepresentative.setSubscriberId(subscriber.getId());

                aRepresentative.setOrganization(selfOrg);

                dbRepresentative.setUserId(user.getId());
                aRepresentative.setPerson(getBecknPerson(user));
                aRepresentative.setContact(getBecknContact(user));

                aRepresentative.setRateable(false);

                aRepresentative.setRole(selfRepresentative.getRole());

                for (UserRole userRole : possibleRepresentative.getUser().getUserRoles()) {

                    if (userRole.getRole().getName().equals("GRO")) {
                        String role = String.format("%s_GRO", baseRole);
                        aRepresentative.setRole(Role.valueOf(role));
                    } else if (userRole.getRole().getName().equals("IRO")) {
                        String role = String.format("%s_PLATFORM", baseRole);
                        aRepresentative.setRole(Role.valueOf(role));
                    } else if (userRole.getRole().getName().equals("ODR")) {
                        String role = String.format("%s_ODR", baseRole);
                        aRepresentative.setRole(Role.valueOf(role));
                    }
                    dbRepresentative.setRole(aRepresentative.getRole().name());
                    dbRepresentative = Database.getTable(Representative.class).getRefreshed(dbRepresentative);
                    if (dbRepresentative.getRawRecord().isNewRecord()) {
                        dbRepresentative.save();
                        issue.getRepresentatives().add(aRepresentative);
                        rolesPendingToBeAdded.remove(dbRepresentative.getRole());
                        if (selfRepresentative.getRole() == aRepresentative.getRole()){
                            selfRepresentative.setInner(aRepresentative.getInner()); // Is what assigns the right person.
                        }
                    }
                }
            }
        }

        if (issue.getSelectedOdrsList() == null) {
            issue.setSelectedOdrsList(new SelectedOdrsList());
        }
        if (!subscriber.getPreferredOdrs().isEmpty()) {
            SelectedOdrs selectedOdrs = null;
            for (SelectedOdrs x : issue.getSelectedOdrsList()) {
                if (ObjectUtil.equals(x.getRepresentative().getSubscriberId(), selfRepresentative.getSubscriberId())) {
                    selectedOdrs = x;
                    break;
                }
            }
            if (selectedOdrs == null) {
                selectedOdrs = new SelectedOdrs();
                issue.getSelectedOdrsList().add(selectedOdrs);

                selectedOdrs.setRepresentative(selfRepresentative);
                selectedOdrs.setOdrs(new Odrs());

                for (PreferredOdr preferredOdr : subscriber.getPreferredOdrs()) {
                    selectedOdrs.getOdrs().add(getBecknOdr(preferredOdr.getOdr()));
                }
            }
        }

    }

    private Odr getDbOdr(Context context, Issue issue, in.succinct.beckn.Odr odr) {
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
        Organization organization = representative.getOrganization();
        String subscriberId = representative.getSubscriberId();
        if (ObjectUtil.equals(getAdaptor().getSubscriber().getSubscriberId(),representative.getSubscriberId())){
           organization = getAdaptor().getSubscriber().getOrganization();
        }
        Company dbCompany = getAdaptor().createCompany(organization,subscriberId);

        Subscriber subscriber = Database.getTable(Subscriber.class).newRecord();
        subscriber.setSubscriberId(representative.getSubscriberId());
        subscriber.setNetworkId(context.getNetworkId());
        subscriber = Database.getTable(Subscriber.class).getRefreshed(subscriber);
        Application application = null;
        if (!subscriber.getRawRecord().isNewRecord()){
            application = subscriber.getApplication();
        }
        if (application == null){
            application = getAdaptor().createApplication(dbCompany, subscriber.getSubscriberId(), context.getDomain());
        }


        subscriber.setApplicationId(application.getId());
        subscriber.setDomainId(context.getDomain());
        subscriber.save();

        Representative dbRepresentative = Database.getTable(Representative.class).newRecord();

        Contact contact = representative.getContact();
        Person person = representative.getPerson();
        if (contact != null || person != null) {
            User user = Database.getTable(User.class).newRecord();
            if (contact != null) {
                user.setEmail(contact.getEmail());
                user.setPhoneNumber(contact.getPhone());
                user.setName(contact.getEmail());
            }
            if (person != null) {
                user.setLongName(person.getName());
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

    public Request createNetworkResponse(Request becknResponse) {
        return becknResponse;
    }


    public static in.succinct.beckn.Issue getBecknIssue(Issue dbIssue) {
        in.succinct.beckn.Issue issue = new in.succinct.beckn.Issue();
        issue.setId(dbIssue.getIssueId());
        issue.setComplainant(getBecknRepresentative(dbIssue.getComplainant(), Complainant.class));

        issue.setEscalationLevel(EscalationLevel.valueOf(dbIssue.getEscalationLevel()));
        issue.setExpectedResolutionTime(new Time());
        issue.getExpectedResolutionTime().setDuration(Duration.of(dbIssue.getExpectedResolutionTs().getTime() - dbIssue.getCreatedAt().getTime(), ChronoUnit.MILLIS));

        issue.setExpectedResponseTime(new Time());
        issue.getExpectedResponseTime().setDuration(Duration.of(dbIssue.getExpectedResponseTs().getTime() - dbIssue.getCreatedAt().getTime(), ChronoUnit.MILLIS));

        {
            Resolution resolution = new Resolution();
            resolution.setDisputeResolutionRemarks(dbIssue.getDisputeResolutionRemarks());
            resolution.setGroResolutionRemarks(dbIssue.getGroResolutionRemarks());
            resolution.setResolution(dbIssue.getResolution());
            resolution.setResolutionRemarks(dbIssue.getResolutionRemarks());
            if (!dbIssue.getReflector().isVoid(dbIssue.getResolutionAction())) {
                resolution.setResolutionAction(ResolutionAction.valueOf(dbIssue.getResolutionAction()));
            }
            if (!dbIssue.getReflector().isVoid(dbIssue.getResolutionStatus())) {
                resolution.setResolutionStatus(ResolutionStatus.valueOf(dbIssue.getResolutionStatus()));
            }
            if (!resolution.getInner().isEmpty()) {
                issue.setResolution(resolution);
                if (resolution.getResolutionStatus() == ResolutionStatus.RESOLVED && resolution.getResolutionAction() == ResolutionAction.REFUND) {
                    resolution.setRefundAmount(dbIssue.getRefundAmount());
                }
            }
        }
        issue.setResolutionProvider(getBecknRepresentative(dbIssue.getResolutionProvider(), in.succinct.beckn.Representative.class));

        issue.setIssueCategory(IssueCategory.valueOf(dbIssue.getIssueCategory()));
        issue.setIssueSubCategory(IssueSubCategory.valueOf(dbIssue.getIssueSubCategory()));
        issue.setRespondent(getBecknRepresentative(dbIssue.getRespondent(), Respondent.class));
        issue.setOrder(new Order(dbIssue.getOrderJson()));
        issue.setSatisfied(dbIssue.isSatisfactorilyClosed());
        issue.setStatus(Status.valueOf(dbIssue.getStatus()));
        issue.setSelectedOdrsList(new SelectedOdrsList());
        issue.setRepresentatives(new Representatives());

        dbIssue.getRepresentors().forEach(dbRepresentative -> {
            SelectedOdrs selectedOdrs = new SelectedOdrs();
            selectedOdrs.setRepresentative(getBecknRepresentative(dbRepresentative, in.succinct.beckn.Representative.class));
            selectedOdrs.setOdrs(new Odrs());
            for (PreferredOdr preferredOdr : dbRepresentative.getSubscriber().getPreferredOdrs()) {
                Odr dbOdr = preferredOdr.getOdr();
                in.succinct.beckn.Odr becknOdr = getBecknOdr(dbOdr);
                selectedOdrs.getOdrs().add(becknOdr);
            }
            issue.getRepresentatives().add(getBecknRepresentative(dbRepresentative, in.succinct.beckn.Representative.class));
        });
        in.succinct.beckn.Odr finalizedOdr = getBecknOdr(dbIssue.getFinalizedOdr());
        issue.setFinalizedOdr(finalizedOdr);
        issue.setCreatedAt(dbIssue.getCreatedAt());
        issue.setUpdatedAt(dbIssue.getUpdatedAt());

        issue.setNotes(new Notes());

        for (in.succinct.bpp.core.db.model.igm.Note dbNote : dbIssue.getNotes()) {
            Note note = new Note();
            note.setId(dbNote.getNoteId());
            note.setAction(new Action());

            if (ObjectUtil.equals(dbNote.getLoggedByRepresentor().getSubscriber().getSubscriberId(), issue.getComplainant().getSubscriberId())) {
                note.getAction().setComplainantAction(RepresentativeAction.valueOf(dbNote.getAction()));
                note.setCreatedBy(issue.getComplainant());
            } else {
                note.getAction().setRespondentAction(RepresentativeAction.valueOf(dbNote.getAction()));
                Representative representative = dbNote.getLoggedByRepresentor();

                Respondent respondent = new Respondent();
                respondent.setRole(Role.valueOf(representative.getRole()));
                respondent.setContact(getBecknContact(representative.getUser()));
                respondent.setOrganization(getBecknOrganization(representative.getSubscriber()));
                respondent.setPerson(getBecknPerson(representative.getUser()));
                note.setCreatedBy(respondent);
            }
            note.setDescription(new Descriptor());
            note.getDescription().setLongDesc(dbNote.getNotes());
            note.getDescription().setShortDesc(dbNote.getSummary());
            if (!dbNote.getReflector().isVoid(dbNote.getParentNoteId())) {
                note.setParentNoteId(dbNote.getParentNote().getNoteId());
            }
            note.getDescription().setImages(new Images());
            for (NoteAttachment attachment : dbNote.getAttachments()) {
                note.getDescription().getImages().add(attachment.getAttachmentUrl());
            }
            note.setCreatedAt(dbNote.getCreatedAt());
            issue.getNotes().add(note);
        }

        return issue;
    }

    private static in.succinct.beckn.Odr getBecknOdr(Odr dbOdr) {
        if (dbOdr == null) {
            return null;
        }
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


    private static <T extends in.succinct.beckn.Representative> T getBecknRepresentative(Subscriber subscriber, Class<T> representativeClass) {
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

    private static <T extends in.succinct.beckn.Representative> T getBecknRepresentative(Representative dbRepresentative, Class<T> clazz) {

        T representative = null;
        try {
            representative = clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        representative.setRole(Role.valueOf(dbRepresentative.getRole()));
        representative.setOrganization(getBecknOrganization(dbRepresentative.getSubscriber()));
        representative.setContact(getBecknContact(dbRepresentative.getUser()));
        representative.setPerson(getBecknPerson(dbRepresentative.getUser()));
        representative.setSubscriberId(dbRepresentative.getSubscriber().getSubscriberId());
        return representative;
    }

    private static Person getBecknPerson(User user) {
        Person person = new Person();
        person.setName(user.getName());
        return person;
    }

    private static Contact getBecknContact(User user) {
        Contact contact = new Contact();
        contact.setEmail(user.getEmail());
        contact.setPhone(user.getPhoneNumber());
        return contact;
    }

    public static Organization getBecknOrganization(Subscriber subscriber) {
        Company company = subscriber.getApplication().getCompany();
        Organization organization = new Organization();
        organization.setName(company.getName());
        organization.setDateOfIncorporation(company.getDateOfIncorporation());
        return organization;
    }


}
