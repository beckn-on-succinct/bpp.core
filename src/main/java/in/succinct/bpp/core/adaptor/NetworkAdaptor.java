package in.succinct.bpp.core.adaptor;

import in.succinct.beckn.BecknAware;
import in.succinct.beckn.BecknObject;
import org.json.JSONObject;
import org.json.simple.JSONAware;

import java.util.List;

public class NetworkAdaptor {

    public <O extends BecknObject,V extends BecknAware<? extends  JSONAware>>
        void set(O becknObject, String name , V value){
        becknObject.set(name,value);
    }


}
