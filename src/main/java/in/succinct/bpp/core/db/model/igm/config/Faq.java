package in.succinct.bpp.core.db.model.igm.config;

import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;
import in.succinct.bpp.core.db.model.Subscriber;

public interface Faq extends Model {
    @PARTICIPANT
    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();


    public String getQuestion();
    public void setQuestion(String question);

    public String getAnswer();
    public void setAnswer(String answer);

}
