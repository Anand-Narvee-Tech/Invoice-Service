package com.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "manual_invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vendorId;
    private String companyName;
    private String companyEmail;
    private String companyPhone;
    private String companyAddress;
    private String companyUrl;
    private String customer;
    private String clientEmail;
    private String customerEmail;
    private String customerPhone;
    private String clientPhone;
    private String billingAddress;
    private String shippingAddress;
    private String salesRep;
    private String invoiceNumber;
    
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate invoiceDate;

//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dueDate;
    
    private String paymentTerms;
    private String poNumber;
    private String status;
    private String template;
    private String termsAndConditions;
    private String notes;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "manual_invoice_files", joinColumns = @JoinColumn(name = "invoice_id"))
    @Column(name = "file_name")
    private List<String> uploadedFileNames = new ArrayList<>();

    private Double totalHours = 0.0;
    private Double subtotal = 0.0;
    private Double tax = 0.0;
    private Double total = 0.0;
    private Double amountDue = 0.0;
    private Double credit = 0.0;

    private String currency;
    private String issuedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "manualInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<InvoiceItem> items;

    /**
     * Updates the current ManualInvoice with values from another invoice.
     * Does not update the id.
     */
    public void updateFrom(ManualInvoice invoice) {
        this.vendorId = invoice.getVendorId();
        this.companyName = invoice.getCompanyName();
        this.companyEmail = invoice.getCompanyEmail();
        this.companyPhone = invoice.getCompanyPhone();
        this.companyAddress = invoice.getCompanyAddress();
        this.companyUrl = invoice.getCompanyUrl();
        this.customer = invoice.getCustomer();
        this.clientEmail = invoice.getClientEmail();
        this.customerEmail = invoice.getCustomerEmail();
        this.customerPhone = invoice.getCustomerPhone();
        this.clientPhone = invoice.getClientPhone();
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
