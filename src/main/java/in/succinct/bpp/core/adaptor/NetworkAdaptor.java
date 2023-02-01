package in.succinct.bpp.core.adaptor;


import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectHolder;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.routing.Config;
import com.venky.swf.views.controls.page.layout.headings.H;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.BecknObjectWithId;
import in.succinct.beckn.BecknObjectsWithId;
import in.succinct.beckn.Request;
import in.succinct.beckn.Subscriber;
import in.succinct.bpp.core.adaptor.api.NetworkApiAdaptor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class NetworkAdaptor extends BecknObjectWithId {
    protected NetworkAdaptor(String networkName){
        setId(networkName);
        getInner().putAll(getConfig());
    }
    public  JSONObject getConfig(){
        try {
            return  (JSONObject) JSONValue.parseWithException(new InputStreamReader(
                Objects.requireNonNull(Config.class.getResourceAsStream(String.format("/config/networks/%s.json", getId())))));
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }




    public boolean isSelfRegistrationSupported(){
        return getBoolean("self_registration_supported");
    }
    public void setSelfRegistrationSupported(boolean self_registration_supported){
        set("self_registration_supported",self_registration_supported);
    }

    public Domains getDomains(){
        return get(Domains.class, "domains");
    }
    public void setDomains(Domains domains){
        set("domains",domains);
    }

    public Subscriber getRegistry(){
        List<Subscriber> subscribers =  lookup(getId(),true);
        if (!subscribers.isEmpty()){
            return subscribers.get(0);
        }
        return null;
    }

    public String getRegistryUrl(){
        return get("registry_url");
    }
    public void setRegistryUrl(String registry_url){
        set("registry_url",registry_url);
    }

    public String getBaseUrl(){
        return get("base_url");
    }
    public void setBaseUrl(String base_url){
        set("base_url",base_url);
    }

    public List<Subscriber> lookup(String subscriberId, boolean onlyIfSubscribed) {
        Subscriber subscriber = new Subscriber();
        subscriber.setSubscriberId(subscriberId);
        return lookup(subscriber,onlyIfSubscribed);
    }


    public long getKeyValidityMillis(){
        return Duration.ofDays(365*10).toMillis();
    }

    public CryptoKey getKey(String uniqueKeyId, String purpose){
        return CryptoKey.find(uniqueKeyId,purpose);
    }
    public void rotateKeys(Subscriber subscriber) {
        CryptoKey existingKey = getKey(subscriber.getUniqueKeyId(),CryptoKey.PURPOSE_SIGNING);

        if (existingKey.getRawRecord().isNewRecord() || existingKey.getUpdatedAt().getTime() + getKeyValidityMillis() <= System.currentTimeMillis() ){
            KeyPair signPair = Crypt.getInstance().generateKeyPair(Request.SIGNATURE_ALGO, Request.SIGNATURE_ALGO_KEY_LENGTH);
            KeyPair encPair = Crypt.getInstance().generateKeyPair(Request.ENCRYPTION_ALGO, Request.ENCRYPTION_ALGO_KEY_LENGTH);

            String keyNumber = existingKey.getAlias().substring(existingKey.getAlias().lastIndexOf('.')+2);// .k[0-9]*
            int nextKeyNumber = Integer.parseInt(keyNumber) + 1;
            String nextKeyId = String.format("%s.k%d",
                    existingKey.getAlias().substring(0,existingKey.getAlias().lastIndexOf('.')),
                    nextKeyNumber);

            subscriber.setUniqueKeyId(nextKeyId);

            CryptoKey signKey = getKey(subscriber.getUniqueKeyId(),CryptoKey.PURPOSE_SIGNING) ;//Create new key
            signKey.setAlgorithm(Request.SIGNATURE_ALGO);
            signKey.setPrivateKey(Crypt.getInstance().getBase64Encoded(signPair.getPrivate()));
            signKey.setPublicKey(Crypt.getInstance().getBase64Encoded(signPair.getPublic()));
            signKey.save();

            CryptoKey encryptionKey = getKey(subscriber.getUniqueKeyId(),CryptoKey.PURPOSE_ENCRYPTION);
            encryptionKey.setAlgorithm(Request.ENCRYPTION_ALGO);
            encryptionKey.setPrivateKey(Crypt.getInstance().getBase64Encoded(encPair.getPrivate()));
            encryptionKey.setPublicKey(Crypt.getInstance().getBase64Encoded(encPair.getPublic()));
            encryptionKey.save();
        }
    }
    public JSONObject getSubscriptionJson(Subscriber subscriber) {
        if (!ObjectUtil.isVoid(subscriber.getDomain()) && (getDomains() == null || getDomains().get(subscriber.getDomain()) == null)){
            throw new RuntimeException("Registry does not support domain " + subscriber.getDomain());
        }
        rotateKeys(subscriber);
        CryptoKey skey = getKey(subscriber.getUniqueKeyId(),CryptoKey.PURPOSE_SIGNING);
        CryptoKey ekey = getKey(subscriber.getUniqueKeyId(),CryptoKey.PURPOSE_ENCRYPTION);

        long validFrom = skey.getUpdatedAt().getTime();
        long validTo = (validFrom + getKeyValidityMillis());

        JSONObject object = new JSONObject();
        object.put("subscriber_id", subscriber.getSubscriberId());
        object.put("subscriber_url", subscriber.getSubscriberUrl());
        object.put("type",subscriber.getType());

        object.put("domain", subscriber.getDomain());
        object.put("signing_public_key", Request.getRawSigningKey(skey.getPublicKey()));
        object.put("encr_public_key", Request.getRawEncryptionKey(ekey.getPublicKey()));
        object.put("valid_from", BecknObject.TIMESTAMP_FORMAT.format(new Date(validFrom)));
        object.put("valid_until", BecknObject.TIMESTAMP_FORMAT.format(new Date(validTo)));
        object.put("country", subscriber.getCountry());
        if (!ObjectUtil.isVoid(subscriber.getCity())){
            object.put("city", subscriber.getCity());
        }
        object.put("created",BecknObject.TIMESTAMP_FORMAT.format(skey.getCreatedAt()));
        object.put("updated",BecknObject.TIMESTAMP_FORMAT.format(skey.getUpdatedAt()));


        object.put("unique_key_id", skey.getAlias());
        object.put("pub_key_id", skey.getAlias());
        object.put("nonce", Base64.getEncoder().encodeToString(String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)));
        subscriber.setInner(object);

        return object;
    }

    public void register(Subscriber subscriber) {
        Request request = new Request(getSubscriptionJson(subscriber));

        Call<JSONObject> call = new Call<JSONObject>().url(getRegistryUrl(), "register").
                method(HttpMethod.POST).input(request.getInner()).inputFormat(InputFormat.JSON).
                header("Content-Type", MimeType.APPLICATION_JSON.toString()).
                header("Accept", MimeType.APPLICATION_JSON.toString());

        JSONObject response = call.getResponseAsJson();
        Config.instance().getLogger(getClass().getName()).info("register" + "-" + response.toString());
    }

    public void subscribe(Subscriber subscriber) {
        if (lookup(subscriber.getSubscriberId(),false).isEmpty()){
            if (isSelfRegistrationSupported()) {
                register(subscriber);
            }
        }
        Request request = new Request(getSubscriptionJson(subscriber));

        Call<JSONObject> call = new Call<JSONObject>().url(getRegistryUrl(), "subscribe").
                method(HttpMethod.POST).input(request.getInner()).inputFormat(InputFormat.JSON).
                header("Content-Type", MimeType.APPLICATION_JSON.toString()).
                header("Accept", MimeType.APPLICATION_JSON.toString());

        call.header("Authorization", request.generateAuthorizationHeader(subscriber.getSubscriberId(),
                Objects.requireNonNull(getKey(subscriber.getUniqueKeyId(),CryptoKey.PURPOSE_SIGNING)).getAlias()));
        JSONObject response = call.getResponseAsJson();
        Config.instance().getLogger(getClass().getName()).info("subscribe" + "-" + response.toString());
    }

    public List<Subscriber> lookup(Subscriber subscriber,boolean onlyIfSubscribed) {
        List<Subscriber> subscribers = new ArrayList<>();

        JSONArray responses = new Call<JSONObject>().method(HttpMethod.POST).url(getRegistryUrl(), "lookup").input(subscriber.getInner()).inputFormat(InputFormat.JSON)
                .header("content-type", MimeType.APPLICATION_JSON.toString())
                .header("accept", MimeType.APPLICATION_JSON.toString()).getResponseAsJson();
        if (responses == null) {
            return subscribers;
        }

        for (Iterator<?> i = responses.iterator(); i.hasNext(); ) {
            JSONObject object1 = (JSONObject) i.next();
            Subscriber subscriber1 = new Subscriber(object1);
            if (onlyIfSubscribed && !ObjectUtil.equals(subscriber1.getStatus(), "SUBSCRIBED")) {
                i.remove();
            } else {
                subscribers.add(subscriber1);
            }
        }
        return subscribers;
    }


    public static class Domains extends BecknObjectsWithId<Domain>{

        public Domains() {
        }

        public Domains(JSONArray array) {
            super(array);
        }
    }

    public static class Domain  extends BecknObjectWithId {

        public Domain() {
        }

        public Domain(JSONObject object) {
            super(object);
        }

        public String getName(){
            return get("name");
        }
        public void setName(String name){
            set("name",name);
        }

        public String getVersion(){
            return get("version");
        }
        public void setVersion(String version){
            set("version",version);
        }

        public String getSchema(){
            return get("schema");
        }
        public void setSchema(String schema){
            set("schema",schema);
        }

        public String getExtensionPackage(){
            return get("extension_package");
        }
        public void setExtensionPackage(String extension_package){
            set("extension_package",extension_package);
        }
    }

    public abstract NetworkApiAdaptor getApiAdaptor();


    Set<Class<?>> classesWithNoExtension = new HashSet<>();
    public <B extends BecknObject, E extends B> B create(Class<B> clazz , String domainId){
        if (!ObjectUtil.equals(clazz.getPackageName(),"in.succinct.beckn")){
            throw new IllegalArgumentException("only classes  in.succinct.beckn.* are allowed");
        }
        String clazzName = String.format("%s.%s",getDomains().get(domainId).getExtensionPackage(),clazz.getSimpleName());
        try {
            Class<?> extendedClass = classesWithNoExtension.contains(clazz)? clazz : Class.forName(clazzName);
            return (B)extendedClass.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            classesWithNoExtension.add(clazz);
            try {
                return clazz.getConstructor().newInstance();
            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
