package com.example.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.common.RestAPIResponse;
import com.example.entity.ManualInvoice;
import com.example.repository.ManualInvoiceRepository;
import com.example.serviceImpl.ManualInvoiceServiceImpl1;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/manual-invoice")
public class ManualInvoiceController1 {

    @Autowired
    private ManualInvoiceServiceImpl1 serviceImpl1;
    
    @Autowired
    private ManualInvoiceRepository manualInvoiceRepository;

    // Save invoice
    @PostMapping("/save")
    public ResponseEntity<RestAPIResponse> saveInvoice(@RequestBody ManualInvoice invoice) {
        try {
            ManualInvoice savedInvoice = serviceImpl1.saveInvoice(invoice);
            return ResponseEntity.ok(new RestAPIResponse("Success", "Invoice Data Saved Successfully", savedInvoice));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to save invoice: " + e.getMessage(), null));
        }
    }

    // Upload files and attach to invoice
    @PostMapping(value = "/upload/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RestAPIResponse> uploadFiles(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            HttpServletRequest request) {

        try {
            ManualInvoice invoice = serviceImpl1.getInvoiceById(id);
            if (invoice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new RestAPIResponse("Error", "Invoice not found", null));
            }

            List<String> uploadedFiles = serviceImpl1.storeMultipleFiles(files);

            // Merge existing and new files
            List<String> currentFiles = invoice.getUploadedFileNames();
            if (currentFiles == null) currentFiles = new ArrayList<>();
            currentFiles.addAll(uploadedFiles);
            invoice.setUploadedFileNames(currentFiles);

            serviceImpl1.saveInvoice(invoice);

            // Generate downloadable URLs
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            List<String> fileUrls = uploadedFiles.stream()
                    .map(fileName -> baseUrl + "/manual-invoice/view/" + fileName)
                    .collect(Collectors.toList());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("uploadedFiles", uploadedFiles);
            responseData.put("fileDownloadUrls", fileUrls);

            return ResponseEntity.ok(new RestAPIResponse("Success", "Files uploaded successfully", responseData));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to upload files: " + e.getMessage(), null));
        }
    }

    // View single file
    @GetMapping("/view/{filename}")
    public ResponseEntity<Resource> viewFile(@PathVariable String filename) {
        try {
            Resource resource = serviceImpl1.loadFileAsResource(filename);

            // Determine content type based on file extension
            String contentType = "application/octet-stream";
            if (filename.endsWith(".pdf")) contentType = "application/pdf";
            else if (filename.endsWith(".csv")) contentType = "text/csv";
            else if (filename.endsWith(".docx"))
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "inline; filename=\"" + resource.getFilename() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Get invoice by ID + uploaded file URLs
    @GetMapping("/{id}")
    public ResponseEntity<RestAPIResponse> getInvoiceById(@PathVariable Long id, HttpServletRequest request) {
        try {
            ManualInvoice invoice = serviceImpl1.getInvoiceById(id);
            if (invoice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new RestAPIResponse("Error", "Invoice not found", null));
            }

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            List<String> fileUrls = invoice.getUploadedFileNames().stream()
                    .map(fileName -> baseUrl + "/manual-invoice/view/" + fileName)
                    .collect(Collectors.toList());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("invoice", invoice);
            responseData.put("fileDownloadUrls", fileUrls);

            return ResponseEntity.ok(new RestAPIResponse("Success", "Invoice Retrieved Successfully", responseData));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to retrieve invoice: " + e.getMessage(), null));
        }
    }

    // Get all invoices
    @GetMapping("/getall")
    public ResponseEntity<RestAPIResponse> getAllInvoices() {
        try {
            return ResponseEntity.ok(new RestAPIResponse("Success", "All Invoices Retrieved", serviceImpl1.getAllInvoices()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to retrieve invoices: " + e.getMessage(), null));
        }
    }

    // Search invoices with pagination
    @GetMapping("/search")
    public ResponseEntity<RestAPIResponse> searchInvoices(
            @RequestParam(name = "search", required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<ManualInvoice> invoices = serviceImpl1.searchInvoices(keyword, pageable);
            return ResponseEntity.ok(new RestAPIResponse("Success", "Invoice Retrieved", invoices));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to search Invoices: " + e.getMessage(), null));
        }
    }
    
    @GetMapping("/searchAndSort")
    public ResponseEntity<RestAPIResponse> getManualInvoices(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            // Call with all 5 params
            Page<ManualInvoice> invoicePage =
                    serviceImpl1.getAllInvoicesWithPaginationAndSearch(page, size, sortField, sortDir, keyword);

            Map<String, Object> response = new HashMap<>();
            response.put("invoices", invoicePage.getContent());
            response.put("currentPage", invoicePage.getNumber());
            response.put("totalItems", invoicePage.getTotalElements());
            response.put("totalPages", invoicePage.getTotalPages());
            response.put("sortField", sortField);
            response.put("sortDir", sortDir);
            response.put("keyword", keyword);

            return ResponseEntity.ok(new RestAPIResponse("Success", "Invoices retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to fetch Invoices: " + e.getMessage(), null));
        }
    }

    
    @PutMapping("/update-status/{invoiceNumber}")
    public ResponseEntity<String> updateInvoiceStatus(
            @PathVariable String invoiceNumber,
            @RequestBody Map<String, String> payload) {

        String status = payload.get("status");
        ManualInvoice invoice = manualInvoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceNumber));

        invoice.setStatus(status);
        invoice.setUpdatedAt(LocalDateTime.now());
        manualInvoiceRepository.save(invoice);

        return ResponseEntity.ok("Invoice " + invoiceNumber + " status updated to " + status);
    }
    
    
    // Update invoice
    @PutMapping("/{id}")
    public ResponseEntity<RestAPIResponse> updateInvoice(@PathVariable Long id, @RequestBody ManualInvoice invoice) {
        try {
            ManualInvoice updatedInvoice = serviceImpl1.updateInvoice(id, invoice);
            if (updatedInvoice == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new RestAPIResponse("Error", "Invoice not found", null));
            }
            return ResponseEntity.ok(new RestAPIResponse("Success", "Invoice Updated Successfully", updatedInvoice));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to update invoice: " + e.getMessage(), null));
        }
    }

    // Delete invoice
    @DeleteMapping("/{id}")
    public ResponseEntity<RestAPIResponse> deleteInvoice(@PathVariable Long id) {
        try {
            serviceImpl1.deleteInvoice(id);
            return ResponseEntity.ok(new RestAPIResponse("Success", "Invoice Deleted Successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RestAPIResponse("Error", "Failed to delete invoice: " + e.getMessage(), null));
        }
    }
}
