package com.example.repository;

import com.example.entity.ManualInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ManualInvoiceRepository extends JpaRepository<ManualInvoice, Long> {

    // Check if an invoice with the given number exists
    boolean existsByInvoiceNumber(String invoiceNumber);

    // Search invoices by keyword in multiple fields
    @Query("""
    	       SELECT m FROM ManualInvoice m 
    	       WHERE 
    	           LOWER(m.invoiceNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	           LOWER(m.customer) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	           LOWER(m.paymentTerms) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	           LOWER(m.currency) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	           LOWER(m.poNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	           LOWER(m.salesRep) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	           LOWER(m.status) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	           CAST(m.total AS string) LIKE CONCAT('%', :keyword, '%') OR
    	           CAST(m.dueDate AS string) LIKE CONCAT('%', :keyword, '%') """)
    	Page<ManualInvoice> searchInvoices(@Param("keyword") String keyword, Pageable pageable);
}
