package in.succinct.bpp.core.db.model.rating;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;


public interface Rating extends Model {
    @UNIQUE_KEY
    public String getRateableId();
    public void setRateableId(String rateableId);

    @UNIQUE_KEY
    @Enumeration(enumClass = "in.succinct.beckn.Rating.RatingCategory")
    public String getRatingCategory();
    public void setRatingCategory(String ratingCategory);


    public String getRatingValue();
    public void setRatingValue(String ratingValue);

}
