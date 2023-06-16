package in.succinct.bpp.core.db.model;

import com.venky.geo.GeoCoder;
import com.venky.geo.GeoCoordinate;
import com.venky.swf.plugins.collab.db.model.config.PinCode;
import com.venky.swf.routing.Config;
import in.succinct.beckn.BecknException;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.BecknObjects;
import in.succinct.beckn.Category;
import in.succinct.beckn.Contact;
import in.succinct.beckn.Fulfillment.FulfillmentType;
import in.succinct.beckn.FulfillmentStop;
import in.succinct.beckn.Location;
import in.succinct.beckn.Organization;
import in.succinct.beckn.SellerException.DistanceServiceabilityError;
import in.succinct.beckn.SellerException.DropoffLocationServiceabilityError;
import in.succinct.beckn.Time;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.time.Duration;

public class ProviderConfig extends BecknObject {
    public ProviderConfig() {
        super();
    }

    public ProviderConfig(JSONObject object){
        super(object);
    }

    public ProviderConfig(String payload) {
        super(payload);
    }

    public Organization getOrganization(){
        return get(Organization.class, "organization");
    }
    public void setOrganization(Organization organization){
        set("organization",organization);
    }

    public String getStoreName() {
        return get("store_name");
    }

    public void setStoreName(String store_name) {
        set("store_name", store_name);
    }

    public String getOwnerEmail() {
        return get("owner_email");
    }

    public void setOwnerEmail(String owner_email) {
        set("owner_email", owner_email);
    }

    public Category getCategory(){
        return get(Category.class, "category");
    }
    public void setCategory(Category category){
        set("category",category);
    }

    public String getVPA() {
        return get("vpa");
    }

    public void setVPA(String vpa) {
        set("vpa", vpa);
    }


    public Duration getTurnAroundTime() {
        String tat = get("turn_around_time");
        return tat == null ? null : Duration.parse(tat);
    }

    public void setTurnAroundTime(Duration turn_around_time) {
        set("turn_around_time", turn_around_time == null ? null : turn_around_time.toString());
    }

    public Duration getFulfillmentTurnAroundTime() {
        String tat = get("fulfillment_turn_around_time");
        return tat == null ? null : Duration.parse(tat);
    }

    public void setFulfillmentTurnAroundTime(Duration fulfillment_turn_around_time) {
        set("fulfillment_turn_around_time", fulfillment_turn_around_time == null ? null : fulfillment_turn_around_time.toString());
    }

    public boolean isReturnSupported() {
        return getBoolean("return_supported");
    }

    public void setReturnSupported(boolean return_supported) {
        set("return_supported", return_supported);
    }

    public boolean isReturnPickupSupported() {
        return getBoolean("return_pickup_supported");
    }

    public void setReturnPickupSupported(boolean return_pickup_supported) {
        set("return_pickup_supported", return_pickup_supported);
    }

    public Duration getReturnWindow() {
        String rw = get("return_window");
        return rw == null ? null : Duration.parse(rw);
    }

    public void setReturnWindow(Duration return_window) {
        set("return_window", return_window == null ? null : return_window.toString());
    }

    public boolean isCodSupported() {
        return getBoolean("cod_supported"); //Default is false
    }

    public void setCodSupported(boolean cod_supported) {
        set("cod_supported", cod_supported);
    }

    public Location getLocation() {
        return get(Location.class, "location");
    }

    public void setLocation(Location location) {
        set("location", location);
    }

    public Contact getSupportContact() {
        return get(Contact.class, "contact");
    }

    public void setSupportContact(Contact contact) {
        set("contact", contact);
    }

    public double getMaxAllowedCommissionPercent(){
        return getDouble("max_allowed_commission_percent");
    }
    public void setMaxAllowedCommissionPercent(double max_allowed_commission_percent){
        set("max_allowed_commission_percent",max_allowed_commission_percent);
    }

    public Integer getMaxOrderQuantity(){
        return getInteger("max_order_quantity",null);
    }
    public void setMaxOrderQuantity(Integer max_order_quantity){
        set("max_order_quantity",max_order_quantity);
    }


    public BecknObject getLogisticsAppConfig(){
        return get(BecknObject.class, "logistics_app_config");
    }
    public void setLogisticsAppConfig(BecknObject logistics_app_config){
        set("logistics_app_config",logistics_app_config);
    }

    public IssueTrackerConfig getIssueTrackerConfig(){
        return get(IssueTrackerConfig.class, "issue_tracker_config");
    }

    public RatingCollectorConfig getRatingCollectorConfig(){
        return get(RatingCollectorConfig.class,"rating_collector_config");
    }


    public double getGstWithheldPercent(){
        return getDouble("gst_withheld_percent",1.0);
    }
    public void setGstWithheldPercent(double gst_withheld_percent){
        set("gst_withheld_percent",gst_withheld_percent);
    }
    public double getTaxWithheldPercent(){
        return getDouble("tax_withheld_percent",1.0);
    }
    public void setTaxWithheldPercent(double tax_withheld_percent){
        set("tax_withheld_percent",tax_withheld_percent);
    }

    public double getTaxWithheldPercentForBuyerAppFinderFee(){
        return getDouble("tax_withheld_percent_for_buyer_app_finder_fee",1.0);
    }
    public void setTaxWithheldPercentForBuyerAppFinderFee(double tax_withheld_percent_for_buyer_app_finder_fee){
        set("tax_withheld_percent_for_buyer_app_finder_fee",tax_withheld_percent_for_buyer_app_finder_fee);
    }

    public double getBuyerAppCommissionGstPct(){
        return getDouble("buyer_app_commission_gst_pct");
    }
    public void setBuyerAppCommissionGstPct(double buyer_app_commission_gst_pct){
        set("buyer_app_commission_gst_pct",buyer_app_commission_gst_pct);
    }

    public double getDeliveryGstPct(){
        return getDouble("delivery_gst_pct");
    }
    public void setDeliveryGstPct(double delivery_gst_pct){
        set("delivery_gst_pct",delivery_gst_pct);
    }


    public static class IssueTrackerConfig extends BecknObject {

        public String getName(){
            return get("name");
        }
        public void setName(String name){
            set("name",name);
        }

        public String getBaseUrl(){
            return get("base_url");
        }
        public void setBaseUrl(String base_url){
            set("base_url",base_url);
        }

        public String getClientId(){
            return get("client_id");
        }
        public void setClientId(String client_id){
            set("client_id",client_id);
        }

        public String getSecret(){
            return get("secret");
        }
        public void setSecret(String secret){
            set("secret",secret);
        }

        public String getTokenName(){
            return get("token_name");
        }
        public void setTokenName(String token_name){
            set("token_name",token_name);
        }

        public String getTokenValue(){
            return get("token_value");
        }
        public void setTokenValue(String token_value){
            set("token_value",token_value);
        }




    }


    public static class RatingCollectorConfig extends BecknObject {
        public String getName(){
            return get("name");
        }
        public void setName(String name){
            set("name",name);
        }
    }

    public static class Serviceability extends BecknObject{
        public boolean isServiceable(){
            return getBoolean("serviceable");
        }
        public void setServiceable(boolean serviceable){
            set("serviceable",serviceable);
        }

        public double getCharges(){
            return getDouble("charges");
        }
        public void setCharges(double charges){
            set("charges",charges);
        }

        BecknException ex;
        public void setReason(BecknException ex){
            this.ex = ex;
        }
        public BecknException getReason(){
            return ex;
        }

    }


    public Serviceability getServiceability(FulfillmentType inFulfillmentType, FulfillmentStop end, Location storeLocation) {
        DeliveryRules rules = getDeliveryRules();

        Serviceability serviceability = new Serviceability();
        serviceability.setServiceable(false);
        serviceability.setReason(new DropoffLocationServiceabilityError());

        if (end == null){
            serviceability.setServiceable(false);
        }else if (FulfillmentType.store_pickup.matches(inFulfillmentType)) {
            serviceability.setServiceable(true);
            serviceability.setCharges(0);
        }else if (rules == null){
            serviceability.setServiceable(true);
            serviceability.setCharges(0);
        }else {
            Location endLocation = end.getLocation();
            if (endLocation != null){
                GeoCoordinate endGps = endLocation.getGps();
                if (endGps == null && endLocation.getAddress() != null){
                    if (endLocation.getAddress().getPinCode() != null){
                        PinCode pinCode = PinCode.find(endLocation.getAddress().getPinCode());
                        if (pinCode.getLat() == null){
                            endGps = new GeoCoordinate(new GeoCoder().getLocation(pinCode.getPinCode() , Config.instance().getGeoProviderParams()));
                            pinCode.setLat(endGps.getLat());
                            pinCode.setLng(endGps.getLng());
                            pinCode.save(); //Lazy save
                        }else {
                            endGps = new GeoCoordinate(pinCode.getLat(),pinCode.getLng());
                        }
                    }else {
                        endGps = new GeoCoordinate(new GeoCoder().getLocation(endLocation.getAddress().flatten() , Config.instance().getGeoProviderParams()));
                    }
                }
                if (endGps != null){
                    endLocation.setGps(endGps);
                    GeoCoordinate storeGps = storeLocation.getGps();
                    double distance = endGps.distanceTo(storeGps);
                    for (DeliveryRule rule : rules){
                        if (rule.getMinDistance() <= distance && distance < rule.getMaxDistance() && !rule.isOnActual() && !rule.isDeliveryViaNetwork()){
                            serviceability.setServiceable(true);
                            serviceability.setCharges(rule.getFixedRate() + rule.getRatePerKm() * distance);
                        }
                    }
                    if (!serviceability.isServiceable()){
                        serviceability.setReason(new DistanceServiceabilityError());
                    }
                }
            }
        }

        return serviceability;
    }

    public DeliveryRules getDeliveryRules(){
        return get(DeliveryRules.class, "delivery_rules");
    }
    public void setDeliveryRules(DeliveryRules delivery_rules){
        set("delivery_rules",delivery_rules);
    }

    public Time getTime(){
        return get(Time.class, "time");
    }
    public void setTime(Time time){
        set("time",time);
    }

    public String getFulfillmentProviderName(){
        return get("fulfillment_provider_name");
    }
    public void setFulfillmentProviderName(String fulfillment_provider_name){
        set("fulfillment_provider_name",fulfillment_provider_name);
    }

    public FulfillmentCategory getFulfillmentCategory(){
        String s = get("fulfillment_category");
        return s == null ? null : FulfillmentCategory.find(s);
    }
    public void setFulfillmentCategory(FulfillmentCategory fulfillment_category){
        set("fulfillment_category",fulfillment_category == null ? null : fulfillment_category.toString());
    }
    public enum FulfillmentCategory {

        EXPRESS("Express Delivery"),
        STANDARD("Standard Delivery"),
        SAME_DAY ("Same Day Delivery"),
        IMMEDIATE ("Immediate Delivery"),
        NEXT_DAY("Next Day Delivery");
        String toString;
        FulfillmentCategory(String toString){
            this.toString = toString;
        }

        @Override
        public String toString() {
            return toString;
        }
        static FulfillmentCategory find(String toString){
            for (FulfillmentCategory value : values()) {
                if (value.toString().equals(toString)){
                    return value;
                }
            }
            return null;
        }
    }

    public static class DeliveryRules extends BecknObjects<DeliveryRule> {

        public DeliveryRules() {
        }

        public DeliveryRules(JSONArray value) {
            super(value);
        }
    }

    public static class DeliveryRule extends BecknObject {

        public DeliveryRule() {
        }

        public DeliveryRule(JSONObject object) {
            super(object);
        }

        public double getMinDistance(){
            return getDouble("min_distance");
        }
        public void setMinDistance(double min_distance){
            set("min_distance",min_distance);
        }

        public double getMaxDistance(){
            return getDouble("max_distance");
        }
        public void setMaxDistance(double max_distance){
            set("max_distance",max_distance);
        }

        public double getRatePerKm(){
            return getDouble("rate_per_km");
        }
        public void setRatePerKm(double rate){
            set("rate",rate);
        }

        public double getFixedRate(){
            return getDouble("fixed_rate");
        }
        public void setFixedRate(double fixed_rate){
            set("fixed_rate",fixed_rate);
        }



        public boolean isOnActual(){
            return getBoolean("on_actual");
        }
        public void setOnActual(boolean on_actual){
            set("on_actual",on_actual);
        }


        public boolean isDeliveryViaNetwork() {
            return getBoolean("delivery_via_network");
        }

        public void setDeliveryViaNetwork(boolean delivery_via_network) {
            set("delivery_via_network", delivery_via_network);
        }



    }
    /* */
    public boolean isStorePickupSupported(){
        return getBoolean("store_pickup_supported");
    }
    public void setStorePickupSupported(boolean store_pickup_supported){
        set("store_pickup_supported",store_pickup_supported);
    }

    public boolean isHomeDeliverySupported(){
        return getBoolean("home_delivery_supported");
    }
    public void setHomeDeliverySupported(boolean home_delivery_supported){
        set("home_delivery_supported",home_delivery_supported);
    }

    public String getGstIn(){
        return get("gst_in");
    }
    public void setGstIn(String gst_in){
        set("gst_in",gst_in);
    }

    public String getFssaiRegistrationNumber(){
        return get("fssai_registration_number");
    }
    public void setFssaiRegistrationNumber(String fssai_registration_number){
        set("fssai_registration_number",fssai_registration_number);
    }

    public String getLogo(){
        return get("logo");
    }
    public void setLogo(String logo){
        set("logo",logo);
    }

}