package com.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.DTO.VendorAddressDTO;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class ManualInvoice {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "manual_invoice_seq")
	@SequenceGenerator(
	    name = "manual_invoice_seq",
	    sequenceName = "manual_invoice_seq",
	    allocationSize = 1)
	private Long id;


    // Customer info
    private String customer;
    private String customerEmail;
    private String customerPhone;

    // Invoice info
    private String template;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String paymentTerms;
    private String poNumber;
    private String salesRep;
    private String status;
    private String termsAndConditions;
    private String notes;

    // Financial info
    private Double totalHours = 0.0;
    private Double subtotal = 0.0;
    private Double tax = 0.0;
    private Double total = 0.0;
    private Double amountDue = 0.0;
    private Double credit = 0.0;
    private String currency;
    private String issuedBy;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Billing Address
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "billing_street")),
            @AttributeOverride(name = "suite", column = @Column(name = "billing_suite")),
            @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
            @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "billing_zip_code"))
    })
    private VendorAddressDTO billingAddress;

    // Shipping Address (can accept string from frontend)
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "shipping_street")),
        @AttributeOverride(name = "suite", column = @Column(name = "shipping_suite")),
        @AttributeOverride(name = "city", column = @Column(name = "shipping_city")),
        @AttributeOverride(name = "state", column = @Column(name = "shipping_state")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "shipping_zip_code"))
    })
    private VendorAddressDTO shippingAddress;


    // Items
    @OneToMany(
    	    mappedBy = "manualInvoice",
    	    cascade = CascadeType.ALL,
    	    orphanRemoval = true
    	)
    	private List<InvoiceItem> items = new ArrayList<>();


    // Uploaded files
    @ElementCollection
    private List<String> uploadedFileNames = new ArrayList<>();

    /**
     * Setter for shippingAddress to handle frontend string input
     */
    public void setShippingAddress(Object shippingAddress) {
        if (shippingAddress instanceof String) {
            this.shippingAddress = new VendorAddressDTO((String) shippingAddress);
        } else if (shippingAddress instanceof VendorAddressDTO) {
            this.shippingAddress = (VendorAddressDTO) shippingAddress;
        }
    }

    /**
     * Setter for billingAddress if needed
     */
    public void setBillingAddress(VendorAddressDTO billingAddress) {
        this.billingAddress = billingAddress;
    }

    /**
     * Update current invoice from another invoice object
     */
    public void updateFrom(ManualInvoice invoice) {
        this.customer = invoice.getCustomer();
        this.customerEmail = invoice.getCustomerEmail();
        this.customerPhone = invoice.getCustomerPhone();
        this.billingAddress = invoice.getBillingAddress();
        this.shippingAddress = invoice.getShippingAddress();
        this.salesRep = invoice.getSalesRep();
        this.invoiceNumber = invoice.getInvoiceNumber();
        this.invoiceDate = invoice.getInvoiceDate();
        this.dueDate = invoice.getDueDate();
        this.paymentTerms = invoice.getPaymentTerms();
        this.poNumber = invoice.getPoNumber();
        this.status = invoice.getStatus();
        this.template = invoice.getTemplate();
        this.termsAndConditions = invoice.getTermsAndConditions();
        this.notes = invoice.getNotes();
        this.items = invoice.getItems();
        this.uploadedFileNames = invoice.getUploadedFileNames();
        this.totalHours = invoice.getTotalHours();
        this.subtotal = invoice.getSubtotal();
        this.tax = invoice.getTax();
        this.total = invoice.getTotal();
        this.amountDue = invoice.getAmountDue();
        this.credit = invoice.getCredit();
        this.currency = invoice.getCurrency();
        this.issuedBy = invoice.getIssuedBy();
        this.createdAt = invoice.getCreatedAt();
        this.updatedAt = invoice.getUpdatedAt();
    }
}
