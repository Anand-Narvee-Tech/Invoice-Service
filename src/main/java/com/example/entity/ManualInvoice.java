package com.example.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.DTO.VendorAddressDTO;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "manual_invoices",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "po_number")
    }
)
@Data
@NoArgsConstructor
public class ManualInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "manual_invoice_seq")
    @SequenceGenerator(
        name = "manual_invoice_seq",
        sequenceName = "manual_invoice_seq",
        allocationSize = 1
    )
    private Long id;
    
    private Long customerVendorId;

    // Customer info
    private String customer;
    private String customerEmail;
    private String customerPhone;

    // Invoice info
    private String template;
    private String invoiceNumber;
    
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate invoiceDate;

//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dueDate;
    
    private String paymentTerms;

    @Column(name = "po_number", nullable = false)
    private String poNumber;

    private String salesRep;
    private String status;
    private String termsAndConditions;
    private String notes;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "manual_invoice_files", joinColumns = @JoinColumn(name = "invoice_id"))
    @Column(name = "file_name")
    private List<String> uploadedFileNames = new ArrayList<>();

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

    // Shipping Address
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
    @NotEmpty(message = "Invoice must contain at least one item")
    @Valid
    private List<InvoiceItem> items = new ArrayList<>();



    // ================= Helper Methods =================

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setManualInvoice(this);
    }

    public void clearItems() {
        items.clear();
    }

//    // Custom setters for frontend compatibility
//    public void setShippingAddress(Object shippingAddress) {
//        if (shippingAddress instanceof String) {
//            this.shippingAddress = new VendorAddressDTO((String) shippingAddress);
//        } else if (shippingAddress instanceof VendorAddressDTO) {
//            this.shippingAddress = (VendorAddressDTO) shippingAddress;
//        }
//    }
//
//    public void setBillingAddress(VendorAddressDTO billingAddress) {
//        this.billingAddress = billingAddress;
//    }
}
