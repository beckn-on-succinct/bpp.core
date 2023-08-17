package in.succinct.bpp.core.db.model.igm.config;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.validations.RegEx;
import com.venky.swf.db.model.Model;
import in.succinct.bpp.core.db.model.Subscriber;

public interface Odr extends Model {
    public String getName();
    public void setName(String name);

    public String getAboutInfo();
    public void setAboutInfo(String aboutInfo);

    public String getUrl();
    public void setUrl(String url);


    public double getPrice();
    public void setPrice(double price);

    @COLUMN_DEF(value = StandardDefault.SOME_VALUE , args = "INR")
    public String getCurrency();
    public void setCurrency(String currency);


    public String getPricingInfo();
    public void setPricingInfo(String pricingInfo);


    @RegEx("[0-9]*.?[0-9]*%")
    public String getRatingValue();
    public void setRatingValue(String ratingValue);

    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();
    //This odr's subscriber.

}
