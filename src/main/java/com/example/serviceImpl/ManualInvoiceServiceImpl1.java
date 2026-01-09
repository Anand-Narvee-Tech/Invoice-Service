package com.example.serviceImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.DTO.VendorDTO;
import com.example.client.VendorFeignClient;
import com.example.entity.InvoiceItem;
import com.example.entity.ManualInvoice;
import com.example.repository.ManualInvoiceRepository;
import com.example.service.ManualInvoiceService1;

import jakarta.transaction.Transactional;



@Service
public class ManualInvoiceServiceImpl1 implements ManualInvoiceService1 {

    private static final String UPLOAD_DIR = "C:\\Users\\admin\\git\\Invoice-Service\\uploads";

    @Autowired
    private ManualInvoiceRepository invoiceRepository;
    
    @Autowired
    private VendorFeignClient vendorFeignClient;


    @Override
    @Transactional
    public ManualInvoice saveInvoice(ManualInvoice request) {

        ManualInvoice invoice;

        // 🔥 UPDATE vs CREATE
        if (request.getId() != null) {
            invoice = invoiceRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
        } else {
            invoice = new ManualInvoice();
            invoice.setCreatedAt(LocalDateTime.now());
        }

        // 🔥 Copy allowed fields
        invoice.setCustomer(request.getCustomer());
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setPaymentTerms(request.getPaymentTerms());
        invoice.setNotes(request.getNotes());
        invoice.setTax(request.getTax());
        invoice.setCredit(request.getCredit());
        invoice.setBillingAddress(request.getBillingAddress());
        invoice.setShippingAddress(request.getShippingAddress());
        invoice.setSalesRep(request.getSalesRep());
        invoice.setPoNumber(request.getPoNumber());
        invoice.setTemplate(request.getTemplate());
        invoice.setTermsAndConditions(request.getTermsAndConditions());
        
        invoice.setStatus(request.getStatus());
        invoice.setCurrency(request.getCurrency());

        // 🔥 ALWAYS enrich from vendor-service
        if (invoice.getCustomer() != null && !invoice.getCustomer().isBlank()) {
            List<VendorDTO> vendors =
                    vendorFeignClient.searchVendors(invoice.getCustomer());

            if (!vendors.isEmpty()) {
                VendorDTO vendor = vendors.get(0);
                invoice.setCustomer(vendor.getVendorName());
                invoice.setCustomerEmail(vendor.getEmail());
                invoice.setCustomerPhone(vendor.getPhoneNumber());
            }
        }

        // 🔥 Items (always set parent)
        if (request.getItems() != null) {
            request.getItems().forEach(item -> item.setManualInvoice(invoice));
            invoice.setItems(request.getItems());
        }

        calculateTotalsAndDueDate(invoice);

        invoice.setUpdatedAt(LocalDateTime.now());

        // 🔥 Invoice number (create only)
        if (invoice.getInvoiceNumber() == null) {
            invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        }

        return invoiceRepository.saveAndFlush(invoice);
    }


        private void calculateTotalsAndDueDate(ManualInvoice invoice) {

            double subtotal = 0.0;
            double totalHours = 0.0;

            if (invoice.getItems() != null) {
                for (InvoiceItem item : invoice.getItems()) {

                    double hours = item.getHours() != null ? item.getHours() : 0.0;
                    double rate = item.getRate() != null ? item.getRate() : 0.0;

                    double amount = hours * rate;
                    item.setAmount(amount);

                    subtotal += amount;
                    totalHours += hours;
                }
            }

            invoice.setSubtotal(subtotal);
            invoice.setTotalHours(totalHours);

            double tax = invoice.getTax() != null ? invoice.getTax() : 0.0;
            invoice.setTotal(subtotal + tax);

            double credit = invoice.getCredit() != null ? invoice.getCredit() : 0.0;
            invoice.setAmountDue(invoice.getTotal() - credit);

            // Calculate due date
            if (invoice.getInvoiceDate() != null && invoice.getPaymentTerms() != null) {
                try {
                    int days = Integer.parseInt(invoice.getPaymentTerms().replaceAll("[^0-9]", ""));
                    invoice.setDueDate(invoice.getInvoiceDate().plusDays(days));
                } catch (Exception e) {
                    invoice.setDueDate(invoice.getInvoiceDate().plusDays(30));
                }
            }
        }
    

    @Override
    public ManualInvoice getInvoiceById(Long id) {
        ManualInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        if (invoice.getUploadedFileNames() == null) {
            invoice.setUploadedFileNames(new ArrayList<>());
        }

        // Verify each file exists
        List<String> existingFiles = new ArrayList<>();
        for (String fileName : invoice.getUploadedFileNames()) {
            File file = new File(UPLOAD_DIR, fileName);
            if (file.exists()) {
                existingFiles.add(fileName);
            }
        }
        invoice.setUploadedFileNames(existingFiles);

        return invoice;
    }

    @Override
    public List<ManualInvoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public Page<ManualInvoice> searchInvoices(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return invoiceRepository.findAll(pageable);
        }
        return invoiceRepository.searchInvoices(keyword, pageable);
    }

    @Override
    public void deleteInvoice(Long id) {
        ManualInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // Delete uploaded files safely
        if (invoice.getUploadedFileNames() != null && !invoice.getUploadedFileNames().isEmpty()) {
            for (String fileName : invoice.getUploadedFileNames()) {
                try {
                    File file = new File(UPLOAD_DIR, fileName);
                    if (file.exists()) {
                        boolean deleted = file.delete();
                        if (!deleted) {
                            System.err.println("⚠️ Warning: Failed to delete file: " + fileName);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error deleting file: " + fileName + " - " + e.getMessage());
                }
            }
        }

        // Clear invoice items (avoid foreign key issues)
        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            invoice.getItems().clear();
        }

        // Save invoice after clearing items (ensures safe deletion)
        invoiceRepository.save(invoice);

        //  Delete invoice
        invoiceRepository.delete(invoice);
    }



    @Override
    @Transactional
    public ManualInvoice updateInvoice(Long id, ManualInvoice request) {

        ManualInvoice existingInvoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        //  Detect customer change
        boolean customerChanged =
                request.getCustomer() != null &&
                !request.getCustomer().equals(existingInvoice.getCustomer());

        //  Update basic fields EXCEPT customer/email/phone
        existingInvoice.setCustomer(request.getCustomer());
        existingInvoice.setInvoiceDate(request.getInvoiceDate());
        existingInvoice.setPaymentTerms(request.getPaymentTerms());
        existingInvoice.setNotes(request.getNotes());
        existingInvoice.setTax(request.getTax());
        existingInvoice.setCredit(request.getCredit());
        existingInvoice.setBillingAddress(request.getBillingAddress());
        existingInvoice.setShippingAddress(request.getShippingAddress());
        existingInvoice.setSalesRep(request.getSalesRep());
        existingInvoice.setPoNumber(request.getPoNumber());
        existingInvoice.setTemplate(request.getTemplate());
        existingInvoice.setTermsAndConditions(request.getTermsAndConditions());
        existingInvoice.setStatus(request.getStatus());
        existingInvoice.setCurrency(request.getCurrency());

        //  Vendor enrichment (SAME AS saveInvoice)
        if (
            (customerChanged) ||
            existingInvoice.getCustomerEmail() == null ||
            existingInvoice.getCustomerEmail().isBlank() ||
            existingInvoice.getCustomerPhone() == null ||
            existingInvoice.getCustomerPhone().isBlank()
        ) {
            if (existingInvoice.getCustomer() != null && !existingInvoice.getCustomer().isBlank()) {
                List<VendorDTO> vendors =
                        vendorFeignClient.searchVendors(existingInvoice.getCustomer());

                if (!vendors.isEmpty()) {
                    VendorDTO vendor = vendors.get(0);
                    existingInvoice.setCustomer(vendor.getVendorName());
                    existingInvoice.setCustomerEmail(vendor.getEmail());
                    existingInvoice.setCustomerPhone(vendor.getPhoneNumber());
                }
            }
        }

        //  Items update
        existingInvoice.getItems().clear();
        if (request.getItems() != null) {
            for (InvoiceItem item : request.getItems()) {
                item.setManualInvoice(existingInvoice);
                existingInvoice.getItems().add(item);
            }
        }

        //  Preserve uploaded files
        if (request.getUploadedFileNames() != null) {
            if (existingInvoice.getUploadedFileNames() == null) {
                existingInvoice.setUploadedFileNames(new ArrayList<>());
            }
            existingInvoice.getUploadedFileNames().addAll(request.getUploadedFileNames());
        }

        calculateTotalsAndDueDate(existingInvoice);

        existingInvoice.setUpdatedAt(LocalDateTime.now());

        return invoiceRepository.save(existingInvoice);
    }



    @Override
    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IOException("Uploaded file is empty!");

        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) dir.mkdirs();

        String originalFilename = file.getOriginalFilename();
        String sanitizedFilename = originalFilename.replaceAll("\\s+", "_");
        String uniqueFilename = System.currentTimeMillis() + "_" + sanitizedFilename;

        File destFile = new File(dir, uniqueFilename);
        file.transferTo(destFile);

        return uniqueFilename;
    }

    @Override
    public List<String> storeMultipleFiles(MultipartFile[] files) throws IOException {
        List<String> savedFiles = new ArrayList<>();
        if (files == null || files.length == 0) throw new IOException("No files provided!");

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String savedFilename = storeFile(file);
                savedFiles.add(savedFilename);
            }
        }
        if (savedFiles.isEmpty()) throw new IOException("All files were empty!");
        return savedFiles;
    }

    @Override
    public List<String> getAllTemplates() {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists() || dir.listFiles() == null) return List.of();

        return Arrays.stream(dir.listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    @Override
    public Resource loadFileAsResource(String filename) throws Exception {
        File file = new File(UPLOAD_DIR, filename);
        if (!file.exists()) throw new FileNotFoundException("File not found: " + filename);
        return new UrlResource(file.toURI());
    }
    
    
    
    public Page<ManualInvoice> getAllInvoicesWithPaginationAndSearch(
            int page, int size, String sortField, String sortDir, String keyword) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                     Sort.by(sortField).ascending() : Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return invoiceRepository.searchInvoices(keyword, pageable);
    }

	@Override
	public ManualInvoice updateManualInvoice(Long id, ManualInvoice invoice) {
		ManualInvoice existingInvoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // Update fields using your entity method
        existingInvoice.updateFrom(invoice);
        existingInvoice.setUpdatedAt(LocalDateTime.now());

        return invoiceRepository.save(existingInvoice);
	}

}
