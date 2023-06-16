package in.succinct.bpp.core.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import in.succinct.beckn.Context;
import in.succinct.beckn.Fulfillment;
import in.succinct.beckn.Order;
import in.succinct.beckn.Provider;
import in.succinct.beckn.Rating;
import in.succinct.beckn.Rating.RatingAck;
import in.succinct.beckn.Rating.RatingCategory;
import in.succinct.beckn.Rating.Ratings;
import in.succinct.beckn.Request;
import in.succinct.beckn.SellerException;
import in.succinct.beckn.SellerException.EntityToRateNotFound;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.rating.RatingCollector;
import in.succinct.bpp.core.adaptor.rating.RatingCollectorFactory;
import in.succinct.bpp.core.db.model.LocalOrderSynchronizerFactory;

public class SuccinctRatingCollector extends RatingCollector {
    static {
        RatingCollectorFactory.getInstance().registerAdaptor("default",SuccinctRatingCollector.class);
    }
    public SuccinctRatingCollector(CommerceAdaptor adaptor){
        super(adaptor);
    }

    @Override
    public void rating(Request request, Request response) {
        Context context = request.getContext();
        String localOrderId = LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(getAdaptor().getSubscriber()).getLocalOrderId(context.getTransactionId());
        if (localOrderId == null){
            throw new SellerException.InvalidOrder();
        }

        Order lastKnown = LocalOrderSynchronizerFactory.getInstance().getLocalOrderSynchronizer(getAdaptor().getSubscriber()).getLastKnownOrder(context.getTransactionId());
        if (lastKnown == null ){
            throw new SellerException.InvalidOrder();
        }
        if (ObjectUtil.isVoid(lastKnown.getId())){
            throw new SellerException.InvalidOrder();
        }

        Ratings ratings = request.getMessage().getRatings();
        if (ratings == null) {
            ratings = new Ratings();
            Rating rating = request.getMessage().getRating();
            ratings.add(rating);
        }
        for (Rating rating : ratings){
            if (rating.getRatingCategory() == RatingCategory.Provider){
                Provider provider = lastKnown.getProvider();
                if (provider == null || !provider.isRateable()){
                    throw new EntityToRateNotFound();
                }
                if (!ObjectUtil.equals(provider.getId(),rating.getId())){
                    throw new EntityToRateNotFound();
                }
            }else if (rating.getRatingCategory() == RatingCategory.Fulfillment){
                Fulfillment fulfillment = lastKnown.getFulfillment();
                if (fulfillment == null || !fulfillment.isRateable()){
                    throw new EntityToRateNotFound();
                }
                if (!ObjectUtil.equals(fulfillment.getId(),rating.getId())){
                    throw new EntityToRateNotFound();
                }
            }else {
                throw new EntityToRateNotFound();
            }

            in.succinct.bpp.core.db.model.rating.Rating dbRating = Database.getTable(in.succinct.bpp.core.db.model.rating.Rating.class).newRecord();
            dbRating.setRateableId(rating.getId());
            dbRating.setRatingCategory(rating.getRatingCategory().name());
            dbRating.setRatingValue(String.valueOf(rating.getValue()));
            dbRating = Database.getTable(in.succinct.bpp.core.db.model.rating.Rating.class).getRefreshed(dbRating);
            dbRating.save();

        }

        RatingAck ratingAck = new RatingAck();
        response.setMessage(ratingAck);
        ratingAck.setRatingAck(true);
    }

}
