package in.succinct.bpp.core.db.model.igm;

import com.venky.swf.db.table.ModelImpl;

public class RepresentativeImpl extends ModelImpl<Representative> {
    public RepresentativeImpl() {
    }

    public RepresentativeImpl(Representative proxy) {
        super(proxy);
    }

    public String getName(){
        Representative representative = getProxy();
        StringBuilder name = new StringBuilder();
        name.append(representative.getRole());
        name.append(" - ");
        name.append(representative.getUser().getName());

        return name.toString();
    }
}
