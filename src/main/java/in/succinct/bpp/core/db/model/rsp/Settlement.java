package in.succinct.bpp.core.db.model.rsp;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import in.succinct.bpp.core.db.model.BecknOrderMeta;
import in.succinct.bpp.core.db.model.Subscriber;

public interface Settlement extends Model {
    @UNIQUE_KEY("O")
    public Long getBecknOrderMetaId();
    public void setBecknOrderMetaId(Long id);
    public BecknOrderMeta getBecknOrderMeta();

    public String getInvoiceNo();
    public void setInvoiceNo(String invoiceNo);


    public double getOrderAmount();
    public void setOrderAmount(double orderAmount);

    public double getDeliveryAmount();
    public void setDeliveryAmount(double deliveryAmount);

    public double getInvoiceAmount();
    public void setInvoiceAmount(double invoiceAmount);

    public double getBuyerFeeAmount();
    public void setBuyerFeeAmount(double buyerFeeAmount);

    public double getTDSWithheld();
    public void setTDSWithheld(double tDSWithheld);

    public double getGSTWithheld();
    public void setGSTWithheld(double gSTWithheld);

    // = tds_wh +gst_wh
    public double getDeductionByCollector();
    public void setDeductionByCollector(double deductionByCollector);

    // invoice_amount - buyer_app_fee - gst_on_buyer_app_fee
    public double getClaimedSettledAmount();
    public void setClaimedSettledAmount(double settledAmount);

    @Enumeration(enumClass = "in.succinct.beckn.SettlementDetail.SettlementType")
    public String getSettlementType();
    public void setSettlementType(String settlementType);

    @Enumeration(enumClass = "in.succinct.beckn.SettlementDetail.SettlementPhase")
    public String getSettlementPhase();
    public void setSettlementPhase(String settlementPhase);

    // claimed_settled_amount - deduction_by_collector.
    public double getClaimedBankCreditAmount();
    public void setClaimedBankCreditAmount(double bankCreditedAmount);


    public double getCGst();
    public void setCGst(double cGst);

    public double getSgst();
    public void setSgst(double sgst);

    public double getIgst();
    public void setIgst(double igst);



    @Enumeration(enumClass = "in.succinct.beckn.Payment.PaymentStatus")
    public String getStatus();
    public void setStatus(String settlementStatus);

    @UNIQUE_KEY("S")
    public String getSettlementReference();
    public void setSettlementReference(String reference);

    @Enumeration(enumClass = "in.succinct.beckn.recon.Settlement.SettledOrder.SettlementReasonCode")
    public String getSettlementReasonCode();
    public void setSettlementReasonCode(String settlementReasonCode);

    public String getCollectionTxnId();
    public void setCollectionTxnId(String collectionTxnId);

    public String getSettlementId();
    public void setSettlementId(String settlementId);

    @Enumeration(enumClass = "in.succinct.beckn.recon.Settlement.SettledOrder.OrderReconStatus")
    public String getOrderReconStatus();
    public void setOrderReconStatus(String orderReconStatus);

    @Enumeration(enumClass = "in.succinct.beckn.recon.Settlement.SettledOrder.ReconStatus")
    public String getReconStatus();
    public void setReconStatus(String reconStatus);

    public Double getDiffAmount();
    public void setDiffAmount(Double diffAmount);

    @Enumeration(enumClass = "in.succinct.beckn.recon.Settlement.SettledOrder.ReconStatus")
    public String getCounterpartyReconStatus();
    public void setCounterpartyReconStatus(String reconStatus);

    public Double getCounterpartyDiffAmount();
    public void setCounterpartyDiffAmount(Double diffAmount);


    public Long getCollectorPartyId();
    public void setCollectorPartyId(Long id);
    public Subscriber getCollectorParty();

    public Long getReceiverPartyId();
    public void setReceiverPartyId(Long id);
    public Subscriber getReceiverParty();


    @COLUMN_DEF(StandardDefault.ZERO)
    @UNIQUE_KEY("O,S")
    public int getSequenceNumber();
    public void setSequenceNumber(int sequenceNumber);

}
