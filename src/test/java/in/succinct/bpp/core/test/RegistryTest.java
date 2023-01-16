package in.succinct.bpp.core.test;

import in.succinct.bpp.core.registry.BecknRegistry;
import org.junit.Test;

public class RegistryTest {

    @Test
    public void testRegistry(){
        System.out.println(new BecknRegistry("https://registry.becknprotocol.io/subscribers",null).getRegistryAlias());
    }
}
