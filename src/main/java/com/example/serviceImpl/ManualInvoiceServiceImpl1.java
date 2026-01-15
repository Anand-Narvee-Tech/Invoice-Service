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
import org.springframework.util.StringUtils;
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

        // ===== CREATE vs UPDATE =====
        if (request.getId() != null && request.getId() > 0) {

            invoice = invoiceRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            // PO number uniqueness (UPDATE)
            if (invoiceRepository.existsByPoNumberAndIdNot(
                    request.getPoNumber(), invoice.getId())) {
                throw new RuntimeException("PO Number already exists");
            }

            invoice.clearItems();

        } else {

            // PO number uniqueness (CREATE)
            if (invoiceRepository.existsByPoNumber(request.getPoNumber())) {
                throw new RuntimeException("PO Number already exists");
            }

            invoice = new ManualInvoice();
            invoice.setCreatedAt(LocalDateTime.now());
        }

        // ===== Field assignment =====
        invoice.setCustomer(request.getCustomer());
        invoice.setCustomerEmail(request.getCustomerEmail());
        invoice.setCustomerPhone(request.getCustomerPhone());
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

        // ===== Items =====
        if (request.getItems() != null) {
            for (InvoiceItem item : request.getItems()) {
                invoice.addItem(item);
            }
        }

        // ===== Calculations =====
        calculateTotalsAndDueDate(invoice);

        invoice.setUpdatedAt(LocalDateTime.now());

        // Invoice number (CREATE only)
        if (invoice.getInvoiceNumber() == null) {
            invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        }

        //  Important: DO NOT use saveAndFlush()
        return invoiceRepository.save(invoice);
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
        public boolean isPoNumberDuplicate(String poNumber, Long invoiceId) {
            if (poNumber == null || poNumber.isBlank()) {
                return false;
            }
            // UPDATE case 
            if (invoiceId != null) {
                return invoiceRepository
                    .existsByPoNumberIgnoreCaseAndIdNot(poNumber, invoiceId);
            }
            // CREATE case
            return invoiceRepository.existsByPoNumberIgnoreCase(poNumber);
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

    @Transactional
    @Override
    public void deleteInvoice(Long id) {

        ManualInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // Delete uploaded files
        if (invoice.getUploadedFileNames() != null) {
            for (String fileName : invoice.getUploadedFileNames()) {
                try {
                    File file = new File(UPLOAD_DIR, fileName);
                    if (file.exists() && !file.delete()) {
                        System.err.println("⚠️ Failed to delete file: " + fileName);
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ File delete error: " + e.getMessage());
                }
            }
        }

        // ONE LINE ONLY
        invoiceRepository.delete(invoice);
    }


    @Override
    @Transactional
    public ManualInvoice updateInvoice(Long id, ManualInvoice request) {

        ManualInvoice existingInvoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // 🔴 PO number duplicate check (exclude current invoice)
        if (request.getPoNumber() != null &&
            invoiceRepository.existsByPoNumberAndIdNot(request.getPoNumber(), id)) {
            throw new RuntimeException("PO Number already exists");
        }

        // 🔍 Detect customer change
        boolean customerChanged =
                request.getCustomer() != null &&
                !request.getCustomer().equals(existingInvoice.getCustomer());

        // 🔄 Update fields
        existingInvoice.setCustomer(request.getCustomer());
        existingInvoice.setCustomerEmail(request.getCustomerEmail());
        existingInvoice.setCustomerPhone(request.getCustomerPhone());
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

        // 🔁 Vendor enrichment (unchanged)
        if (
            customerChanged ||
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

        // 🔁 Items update
        existingInvoice.getItems().clear();
        if (request.getItems() != null) {
            for (InvoiceItem item : request.getItems()) {
                item.setId(null); // ensure INSERT
                item.setManualInvoice(existingInvoice);
                existingInvoice.getItems().add(item);
            }
        }

        // 📎 Update uploaded files ONLY if provided
        if (request.getUploadedFileNames() != null) {
            existingInvoice.setUploadedFileNames(request.getUploadedFileNames());
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
    
    @Transactional
    @Override
    public ManualInvoice updateUploadedFilesOnly(ManualInvoice invoice, List<String> newFiles) {
        List<String> files = invoice.getUploadedFileNames();
        if (files == null) files = new ArrayList<>();
        files.addAll(newFiles);
        invoice.setUploadedFileNames(files);

        // Direct repository save, without recalculating items
        return invoiceRepository.save(invoice);
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
            int page,
            int size,
            String sortField,
            String sortDir,
            String keyword) {

        // Fallback safety
        if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
            sortDir = "asc";
        }

        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ManualInvoice> invoicePage =
                invoiceRepository.searchInvoices(keyword, pageable);

        // Enrich invoice response
        invoicePage.getContent().forEach(this::enrichFromVendorService);

        return invoicePage;
    }


    private void enrichFromVendorService(ManualInvoice invoice) {

        // Skip if customer name missing
        if (!StringUtils.hasText(invoice.getCustomer())) {
            return;
        }

        List<VendorDTO> vendors =
                vendorFeignClient.searchVendors(invoice.getCustomer());

        if (vendors == null || vendors.isEmpty()) {
            return;
        }

        VendorDTO vendor = vendors.get(0);

        // ✅ Set billing address if missing
        if (invoice.getBillingAddress() == null ||
            invoice.getBillingAddress().getStreet() == null) {

            invoice.setBillingAddress(vendor.getVendorAddress());
        }

        // ✅ Sync contact info
        invoice.setCustomerEmail(vendor.getEmail());
        invoice.setCustomerPhone(vendor.getPhoneNumber());
    }


    @Override
    @Transactional
    public ManualInvoice updateManualInvoice(Long id, ManualInvoice request) {

        ManualInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // 🔐 PO number uniqueness check
        if (invoiceRepository.existsByPoNumberAndIdNot(
                request.getPoNumber(), invoice.getId())) {
            throw new RuntimeException("PO Number already exists");
        }

        // ===== Update fields explicitly =====
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

        // ===== Items (VERY IMPORTANT) =====
        invoice.clearItems();
        if (request.getItems() != null) {
            for (InvoiceItem item : request.getItems()) {
                invoice.addItem(item);
            }
        }

        calculateTotalsAndDueDate(invoice);
        invoice.setUpdatedAt(LocalDateTime.now());

        return invoiceRepository.save(invoice);
    }


}
