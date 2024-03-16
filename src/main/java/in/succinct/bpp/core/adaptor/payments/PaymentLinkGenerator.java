package in.succinct.bpp.core.adaptor.payments;

import in.succinct.beckn.Payment;

public interface PaymentLinkGenerator {
    public void updatePaymentLink(Payment payment);
}
