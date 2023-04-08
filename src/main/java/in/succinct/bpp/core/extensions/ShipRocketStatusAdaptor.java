package in.succinct.bpp.core.extensions;

import com.venky.core.date.DateUtils;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import in.succinct.beckn.Fulfillment.FulfillmentStatus;
import in.succinct.beckn.SellerException;
import in.succinct.beckn.SellerException.InvalidOrder;
import in.succinct.bpp.core.adaptor.CommerceAdaptor;
import in.succinct.bpp.core.adaptor.FulfillmentStatusAdaptor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShipRocketStatusAdaptor extends FulfillmentStatusAdaptor {
    ShipRocketStatusAdaptor(CommerceAdaptor commerceAdaptor) {
        super(commerceAdaptor);
    }

    Map<String,String> authHeaders = new HashMap<>();

    public Map<String,String> getAuthHeaders(boolean refresh) {
        if (authHeaders.isEmpty() || refresh) {
            JSONObject response = new Call<JSONObject>().url("https://apiv2.shiprocket.in/v1/external/auth/login").header("content-type", MimeType.APPLICATION_JSON.toString()).input(new JSONObject() {{
                put("email", getCommerceAdaptor().getProviderConfig().getLogisticsAppConfig().get("email"));
                put("password", getCommerceAdaptor().getProviderConfig().getLogisticsAppConfig().get("password"));
            }}).inputFormat(InputFormat.JSON).method(HttpMethod.POST).timeOut(5000).getResponseAsJson();
            authHeaders.put("Authorization", String.format("Bearer %s", response.get("token")));
        }
        return authHeaders;
    }
    Map<Integer,FulfillmentStatus> fulfillmentStatusMap = new HashMap<>(){{
        put(9,FulfillmentStatus.Pending);
        put(19,FulfillmentStatus.Packed);
        put(42,FulfillmentStatus.Order_picked_up);
        put(18,FulfillmentStatus.Order_picked_up);
        put(17,FulfillmentStatus.Out_for_delivery);
        put(7,FulfillmentStatus.Order_delivered);

    }};

    private JSONObject track(String orderNumber){
        Call<JSONObject> call = new Call<JSONObject>().url(String.format("https://apiv2.shiprocket.in/v1/external/courier/track")).method(HttpMethod.GET).
                header("content-type", MimeType.APPLICATION_JSON.toString()).headers(getAuthHeaders(false)).inputFormat(InputFormat.FORM_FIELDS).input(new JSONObject(){{
                    put("order_id",orderNumber);
                    put("channel_id",getCommerceAdaptor().getProviderConfig().getLogisticsAppConfig().get("channel_id"));
                }});
        if (call.hasErrors()){
            call.headers(getAuthHeaders(true));
        }
        JSONArray response =  call.getResponseAsJson();
        if (response.size() != 1){
            return new JSONObject();
        }else{
            JSONObject jsonObject = (JSONObject)(response.get(0));
            return(JSONObject) jsonObject.get("tracking_data");
        }
    }


    @Override
    public List<FulfillmentStatusAudit> getStatusAudit(String orderNumber) {

        DateFormat format = DateUtils.getFormat(DateUtils.ISO_DATE_TIME_FORMAT_STR);

        List<FulfillmentStatusAudit> audits = new ArrayList<>();
        JSONObject trackingData = track(orderNumber);
        JSONArray trackingActivities = (JSONArray) trackingData.get("shipment_track_activities");
        for (Object ta : trackingActivities){
            JSONObject trackingActivity = (JSONObject) ta;
            FulfillmentStatus fulfillmentStatus = fulfillmentStatusMap.get((Integer) trackingActivity.get("sr-status"));
            if (fulfillmentStatus != null){
                FulfillmentStatusAudit statusAudit = new FulfillmentStatusAudit();
                statusAudit.setFulfillmentStatus(fulfillmentStatus);
                try {
                    statusAudit.setDate(format.parse((String)trackingActivity.get("date")));
                    audits.add(statusAudit);
                } catch (ParseException e) {
                    //
                }
            }

        }
        return audits;
    }

    public String getTrackingUrl(String orderNumber) {
        JSONObject trackingData = track(orderNumber);
        return (String)trackingData.get("tracking_url");
    }
}
