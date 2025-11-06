package com.example.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.entity.ManualInvoice;

@Repository
public interface ManualInvoiceRepository extends JpaRepository<ManualInvoice, Long> {

    // Check if an invoice with the given number exists
    boolean existsByInvoiceNumber(String invoiceNumber);

    // Search invoices by keyword in multiple fields
    @Query("""
    	    SELECT m FROM ManualInvoice m 
    	    WHERE 
    	        LOWER(COALESCE(m.invoiceNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	        LOWER(COALESCE(m.customer, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	        LOWER(COALESCE(m.paymentTerms, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	        LOWER(COALESCE(m.currency, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	        LOWER(COALESCE(m.poNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	        LOWER(COALESCE(m.salesRep, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	        LOWER(COALESCE(m.status, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
    	        STR(m.total) LIKE CONCAT('%', :keyword, '%') OR
    	        STR(m.dueDate) LIKE CONCAT('%', :keyword, '%')
    	""")
    	Page<ManualInvoice> searchInvoices(@Param("keyword") String keyword, Pageable pageable);

    
    Optional<ManualInvoice> findByInvoiceNumber(String invoiceNumber);
}
