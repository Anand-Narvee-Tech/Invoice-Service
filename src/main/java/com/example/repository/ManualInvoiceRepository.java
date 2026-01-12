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
//    @Query("SELECT m FROM ManualInvoice m " +
//           "WHERE LOWER(m.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
//           "OR LOWER(m.customer) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
//           "OR LOWER(m.clientEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
//           "OR LOWER(m.clientPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
//           "OR LOWER(m.billingAddress) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
//           "OR LOWER(m.invoiceNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
//           "OR LOWER(m.currency) LIKE LOWER(CONCAT('%', :keyword, '%'))")
//    Page<ManualInvoice> searchInvoices(@Param("keyword") String keyword, Pageable pageable);
//    
    @Query("""
            SELECT m FROM ManualInvoice m
            WHERE
                (:keyword IS NULL OR :keyword = '')
                OR
                (
                    LOWER(COALESCE(m.invoiceNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(m.customer, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(m.paymentTerms, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(m.currency, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(m.poNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(m.salesRep, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(m.status, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              
                    OR (
                        (m.status IS NULL OR m.status = '')
                        AND LOWER(:keyword) = 'pending'
                    )                
                    OR STR(m.invoiceDate) LIKE CONCAT('%', :keyword, '%')
                    OR STR(m.dueDate) LIKE CONCAT('%', :keyword, '%')
                    OR STR(m.total) LIKE CONCAT('%', :keyword, '%')
                )""")
        Page<ManualInvoice> searchInvoices(
                @Param("keyword") String keyword,
                Pageable pageable);
    
    boolean existsByPoNumber(String poNumber);

    boolean existsByPoNumberAndIdNot(String poNumber, Long id);

    
    Optional<ManualInvoice> findByInvoiceNumber(String invoiceNumber);
    
    boolean existsByPoNumberIgnoreCaseAndIdNot(String poNumber, Long id);
    
    boolean existsByPoNumberIgnoreCase(String poNumber);
}
