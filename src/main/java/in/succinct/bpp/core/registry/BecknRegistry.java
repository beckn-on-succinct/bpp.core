package in.succinct.bpp.core.registry;

import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.routing.Config;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.Request;
import in.succinct.beckn.Subscriber;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class BecknRegistry {
    final String url ;
    final String schema;
    public BecknRegistry(String url,String schema){
        this.url = url;
        this.schema = schema;
    }
    public String getUrl(){
        return url;
    }

    public String getSchema(){
        return this.schema;
    }
    public boolean isSelfRegistrationSupported(){
        return true;
    }

    public void register(Subscriber subscriber) {
        Request request = new Request(getSubscriptionJson(subscriber));

        Call<JSONObject> call = new Call<JSONObject>().url(getUrl(), "register").
                method(HttpMethod.POST).input(request.getInner()).inputFormat(InputFormat.JSON).
                header("Content-Type", MimeType.APPLICATION_JSON.toString()).
                header("Accept", MimeType.APPLICATION_JSON.toString());

        JSONObject response = call.getResponseAsJson();
        Config.instance().getLogger(BecknRegistry.class.getName()).info("register" + "-" + response.toString());
    }

    public void subscribe(Subscriber subscriber) {
        if (lookup(subscriber.getSubscriberId()).isEmpty()){
            if (isSelfRegistrationSupported()) {
                register(subscriber);
            }
        }
        Request request = new Request(getSubscriptionJson(subscriber));

        Call<JSONObject> call = new Call<JSONObject>().url(getUrl(), "subscribe").
                method(HttpMethod.POST).input(request.getInner()).inputFormat(InputFormat.JSON).
                header("Content-Type", MimeType.APPLICATION_JSON.toString()).
                header("Accept", MimeType.APPLICATION_JSON.toString());

        call.header("Authorization", request.generateAuthorizationHeader(subscriber.getSubscriberId(),
                Objects.requireNonNull(getKey(subscriber.getUniqueKeyId(),CryptoKey.PURPOSE_SIGNING)).getAlias()));
        JSONObject response = call.getResponseAsJson();
        Config.instance().getLogger(BecknRegistry.class.getName()).info("subscribe" + "-" + response.toString());
    }

    public List<Subscriber> lookup(String subscriberId) {
        Subscriber subscriber = new Subscriber();
        subscriber.setSubscriberId(subscriberId);
        return lookup(subscriber);
    }

    public List<Subscriber> lookup(Subscriber subscriber) {
        List<Subscriber> subscribers = new ArrayList<>();

        JSONArray responses = new Call<JSONObject>().method(HttpMethod.POST).url(getUrl(), "lookup").input(subscriber.getInner()).inputFormat(InputFormat.JSON)
                .header("content-type", MimeType.APPLICATION_JSON.toString())
                .header("accept", MimeType.APPLICATION_JSON.toString()).getResponseAsJson();
        if (responses == null) {
            return subscribers;
        }

        for (Iterator<?> i = responses.iterator(); i.hasNext(); ) {
            JSONObject object1 = (JSONObject) i.next();
            Subscriber subscriber1 = new Subscriber(object1);
            if (!ObjectUtil.equals(subscriber1.getStatus(), "SUBSCRIBED")) {
                i.remove();
            } else {
                subscribers.add(subscriber1);
            }
        }
        return subscribers;
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
}
