package com.example.serviceImpl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.entity.InvoiceItem;
import com.example.entity.ManualInvoice;
import com.example.repository.ManualInvoiceRepository;
import com.example.service.ManualInvoiceService1;

@Service
public class ManualInvoiceServiceImpl1 implements ManualInvoiceService1 {

    @Value("${file.upload-dir}")
    private String uploadDir; // configurable path

    private final ManualInvoiceRepository invoiceRepository;

    public ManualInvoiceServiceImpl1(ManualInvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public ManualInvoice saveInvoice(ManualInvoice invoice) {
        if (invoiceRepository.existsByInvoiceNumber(invoice.getInvoiceNumber())) {
            invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        }
        calculateTotalsAndDueDate(invoice);
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
                item.setManualInvoice(invoice);
            }
        }

        invoice.setSubtotal(subtotal);
        invoice.setTotalHours(totalHours);

        double tax = invoice.getTax() != null ? invoice.getTax() : 0.0;
        double total = subtotal + tax;
        invoice.setTotal(total);

        double credit = invoice.getCredit() != null ? invoice.getCredit() : 0.0;
        invoice.setAmountDue(total - credit);

        if (invoice.getInvoiceDate() != null && invoice.getPaymentTerms() != null) {
            try {
                int days = Integer.parseInt(invoice.getPaymentTerms().replaceAll("[^0-9]", ""));
                invoice.setDueDate(invoice.getInvoiceDate().plusDays(days));
            } catch (NumberFormatException e) {
                invoice.setDueDate(invoice.getInvoiceDate().plusDays(30));
            }
        }
    }

    @Override
    public ManualInvoice getInvoiceById(Long id) {
        ManualInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        if (invoice.getUploadedFileNames() == null) invoice.setUploadedFileNames(new ArrayList<>());

        List<String> existingFiles = new ArrayList<>();
        for (String fileName : invoice.getUploadedFileNames()) {
            Path filePath = Paths.get(uploadDir, fileName);
            if (Files.exists(filePath)) existingFiles.add(fileName);
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
        if (keyword == null || keyword.trim().isEmpty()) return invoiceRepository.findAll(pageable);
        keyword = keyword.trim();
        return invoiceRepository.searchInvoices(keyword, pageable);
    }

    @Override
    public void deleteInvoice(Long id) {
        ManualInvoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        if (invoice.getUploadedFileNames() != null) {
            for (String fileName : invoice.getUploadedFileNames()) {
                try {
                    Path filePath = Paths.get(uploadDir, fileName);
                    Files.deleteIfExists(filePath);
                } catch (Exception e) {
                    System.err.println("⚠️ Error deleting file: " + fileName + " - " + e.getMessage());
                }
            }
        }

        if (invoice.getItems() != null) invoice.getItems().clear();
        invoiceRepository.save(invoice);
        invoiceRepository.delete(invoice);
    }

    @Override
    public ManualInvoice updateInvoice(Long id, ManualInvoice invoice) {
        ManualInvoice existingInvoice = invoiceRepository.findById(id).orElse(null);
        if (existingInvoice == null) return null;

        existingInvoice.updateFrom(invoice);
        existingInvoice.getItems().clear();
        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                item.setManualInvoice(existingInvoice);
                existingInvoice.getItems().add(item);
            }
        }

        if (invoice.getUploadedFileNames() != null) {
            if (existingInvoice.getUploadedFileNames() == null) existingInvoice.setUploadedFileNames(new ArrayList<>());
            existingInvoice.getUploadedFileNames().addAll(invoice.getUploadedFileNames());
        }

        return invoiceRepository.save(existingInvoice);
    }

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IOException("Uploaded file is empty!");

        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) Files.createDirectories(dirPath);

        String originalFilename = file.getOriginalFilename();
        String sanitizedFilename = originalFilename.replaceAll("\\s+", "_");
        String uniqueFilename = System.currentTimeMillis() + "_" + sanitizedFilename;

        Path filePath = dirPath.resolve(uniqueFilename);
        file.transferTo(filePath);

        return uniqueFilename;
    }

    @Override
    public List<String> storeMultipleFiles(MultipartFile[] files) throws IOException {
        List<String> savedFiles = new ArrayList<>();
        if (files == null || files.length == 0) throw new IOException("No files provided!");

        for (MultipartFile file : files) {
            if (!file.isEmpty()) savedFiles.add(storeFile(file));
        }

        if (savedFiles.isEmpty()) throw new IOException("All files were empty!");
        return savedFiles;
    }

    @Override
    public List<String> getAllTemplates() {
        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) return List.of();

        try {
            return Files.list(dirPath)
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    @Override
    public Resource loadFileAsResource(String filename) throws Exception {
        Path filePath = Paths.get(uploadDir, filename);
        if (!Files.exists(filePath)) throw new FileNotFoundException("File not found: " + filename);
        return new UrlResource(filePath.toUri());
    }
    
    
    public Page<ManualInvoice> getAllInvoicesWithPaginationAndSearch(
            int page, int size, String sortField, String sortDir, String keyword) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                     Sort.by(sortField).ascending() : Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        
        if(keyword == null || keyword.trim().isEmpty()) {
        	return invoiceRepository.findAll(pageable);
        }

        return invoiceRepository.searchInvoices(keyword, pageable);
    }
    
    
}
