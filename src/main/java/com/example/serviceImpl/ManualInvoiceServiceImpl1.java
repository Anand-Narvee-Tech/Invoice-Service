package com.example.serviceImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.DTO.ConsultantDTO;
import com.example.DTO.VendorDTO;
import com.example.client.ConsultantFeignClient;
import com.example.client.VendorFeignClient;
import com.example.entity.InvoiceItem;
import com.example.entity.ManualInvoice;
import com.example.repository.ManualInvoiceRepository;
import com.example.service.ManualInvoiceService1;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ManualInvoiceServiceImpl1 implements ManualInvoiceService1 {

	@Value("${file.upload-dir}")
	private String uploadDir;

	@Autowired
	private ManualInvoiceRepository invoiceRepository;

	@Autowired
	private VendorFeignClient vendorFeignClient;

	@Autowired
	private ConsultantFeignClient consultantFeignClient;

	@Autowired
	private InvoiceEmailService invoiceEmailService;

	@Override
	@Transactional
	public ManualInvoice saveInvoice(ManualInvoice request) {

		ManualInvoice invoice;

		// ===== CREATE vs UPDATE =====
		if (request.getId() != null && request.getId() > 0) {

			invoice = invoiceRepository.findById(request.getId())
					.orElseThrow(() -> new RuntimeException("Invoice not found"));

			if (invoiceRepository.existsByPoNumberAndIdNot(request.getPoNumber(), invoice.getId())) {
				throw new RuntimeException("PO Number already exists");
			}

			invoice.clearItems();

		} else {

			if (invoiceRepository.existsByPoNumber(request.getPoNumber())) {
				throw new RuntimeException("PO Number already exists");
			}

			invoice = new ManualInvoice();
			invoice.setCreatedAt(LocalDateTime.now());
		}

		// ===== Validate Consultant =====
		if (request.getConsultantId() != null) {

			ConsultantDTO consultant = consultantFeignClient.getConsultant(request.getConsultantId());

			if (consultant == null) {
				throw new RuntimeException("Consultant not found with id: " + request.getConsultantId());
			}

			invoice.setConsultantId(consultant.getId());
			invoice.setConsultantName(consultant.getFullName());
		}

		// ===== Basic Fields =====
		invoice.setCustomer(request.getCustomer());
		invoice.setCustomerEmail(request.getCustomerEmail());
		invoice.setCustomerPhone(request.getCustomerPhone());
		invoice.setInvoiceDate(request.getInvoiceDate());
		invoice.setDueDate(request.getDueDate());
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

		// ===== Vendor Lookup =====
		if (request.getCustomer() != null && !request.getCustomer().isBlank()) {

			List<VendorDTO> vendors = vendorFeignClient.searchVendors(request.getCustomer());

			if (!vendors.isEmpty()) {

				VendorDTO vendor = vendors.get(0);

				invoice.setCustomerVendorId(vendor.getVendorId());
				invoice.setCustomer(vendor.getVendorName());

				// Use request email if provided
				if (request.getCustomerEmail() != null && !request.getCustomerEmail().isBlank()) {
					invoice.setCustomerEmail(request.getCustomerEmail());
				} else {
					invoice.setCustomerEmail(vendor.getEmail());
				}

				// Use request phone if provided
				if (request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank()) {
					invoice.setCustomerPhone(request.getCustomerPhone());
				} else {
					invoice.setCustomerPhone(vendor.getPhoneNumber());
				}

			} else {
				throw new RuntimeException("Vendor not found for customer: " + request.getCustomer());
			}
		}

		// ===== Items =====
		if (request.getItems() != null) {
			for (InvoiceItem item : request.getItems()) {
				invoice.addItem(item);
			}
		}

		// ===== Calculations =====
		calculateTotalsAndDueDate(invoice);

		invoice.setUpdatedAt(LocalDateTime.now());

		// ===== Generate Invoice Number =====
		if (invoice.getInvoiceNumber() == null) {

			LocalDate today = LocalDate.now();
			String year = String.valueOf(today.getYear()).substring(2);

			Long consultantId = invoice.getConsultantId() != null ? invoice.getConsultantId() : 0L;
			String consultant = String.format("%03d", consultantId);

			invoice = invoiceRepository.save(invoice);

			String invoiceId = String.format("%03d", invoice.getId());

			invoice.setInvoiceNumber("INV-" + year + consultant + invoiceId);
		}

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
//            if (invoice.getInvoiceDate() != null && invoice.getPaymentTerms() != null) {
//                try {
//                    int days = Integer.parseInt(invoice.getPaymentTerms().replaceAll("[^0-9]", ""));
//                    invoice.setDueDate(invoice.getInvoiceDate().plusDays(days));
//                } catch (Exception e) {
//                    invoice.setDueDate(invoice.getInvoiceDate().plusDays(30));
//                }
//            }
	}

	// ✅ New method: fetch by invoiceNumber
	public ManualInvoice getInvoiceByNumber(String invoiceNumber) {
		return invoiceRepository.findByInvoiceNumber(invoiceNumber)
				.orElseThrow(() -> new RuntimeException("Invoice not found with number: " + invoiceNumber));
	}

	@Override
	public boolean isPoNumberDuplicate(String poNumber, Long invoiceId) {
		if (poNumber == null || poNumber.isBlank()) {
			return false;
		}
		// UPDATE case
		if (invoiceId != null) {
			return invoiceRepository.existsByPoNumberIgnoreCaseAndIdNot(poNumber, invoiceId);
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

		List<String> existingFiles = new ArrayList<>();
		for (String fileName : invoice.getUploadedFileNames()) {
			File file = new File(uploadDir, fileName);
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
		if (keyword == null || keyword.trim().isEmpty())
			return invoiceRepository.findAll(pageable);
		keyword = keyword.trim();
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
					File file = new File(uploadDir, fileName);
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

		// 1️⃣ Fetch existing invoice
		ManualInvoice existingInvoice = invoiceRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

		// 2️⃣ PO Number uniqueness check (exclude current invoice)
		if (request.getPoNumber() != null && invoiceRepository.existsByPoNumberAndIdNot(request.getPoNumber(), id)) {
			throw new RuntimeException("PO Number already exists");
		}

		// 3️⃣ Detect if customer/vendor has changed
		boolean customerChanged = request.getCustomerVendorId() != null
				&& !request.getCustomerVendorId().equals(existingInvoice.getCustomerVendorId());

		// 4️⃣ Update basic fields
		existingInvoice.setCustomer(request.getCustomer());
		existingInvoice.setCustomerEmail(request.getCustomerEmail());
		existingInvoice.setCustomerPhone(request.getCustomerPhone());
		existingInvoice.setInvoiceDate(request.getInvoiceDate());
		existingInvoice.setDueDate(request.getDueDate());
		existingInvoice.setPaymentTerms(request.getPaymentTerms());
		existingInvoice.setNotes(request.getNotes());
		existingInvoice.setCustomerVendorId(request.getCustomerVendorId());
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

		// 5️⃣ Vendor enrichment via Feign (using vendorId for accuracy)
		if (customerChanged || existingInvoice.getCustomerEmail() == null
				|| existingInvoice.getCustomerEmail().isBlank() || existingInvoice.getCustomerPhone() == null
				|| existingInvoice.getCustomerPhone().isBlank()) {

			if (existingInvoice.getCustomerVendorId() != null) {
				try {
					VendorDTO vendor = vendorFeignClient.getVendorById(existingInvoice.getCustomerVendorId());
					if (vendor != null) {
						existingInvoice.setCustomer(vendor.getVendorName());
						existingInvoice.setCustomerEmail(vendor.getEmail());
						existingInvoice.setCustomerPhone(vendor.getPhoneNumber());
						existingInvoice.setBillingAddress(vendor.getVendorAddress());
						existingInvoice.setShippingAddress(vendor.getVendorAddress());
					}
				} catch (Exception e) {
					// Log but don’t block update
					log.warn("Vendor enrichment failed for vendorId {}: {}", existingInvoice.getCustomerVendorId(),
							e.getMessage());
				}
			}
		}

		// 6️ Update invoice items
		existingInvoice.getItems().clear();
		if (request.getItems() != null) {
			for (InvoiceItem item : request.getItems()) {
				item.setId(null); // Ensure insert
				item.setManualInvoice(existingInvoice);
				existingInvoice.getItems().add(item);
			}
		}

		// 7️ Update uploaded files if provided
		if (request.getUploadedFileNames() != null) {
			existingInvoice.setUploadedFileNames(request.getUploadedFileNames());
		}

		// 8️ Recalculate totals
		calculateTotalsAndDueDate(existingInvoice);

		// 9️ Update timestamp
		existingInvoice.setUpdatedAt(LocalDateTime.now());

		// Save and return
		return invoiceRepository.save(existingInvoice);
	}

	@Override
	public String storeFile(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new IOException("Uploaded file is empty");
		}

		Path uploadPath = Paths.get(uploadDir).normalize();
		Files.createDirectories(uploadPath);

		String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
		if (originalFileName.contains("..")) {
			throw new IOException("Invalid filename: " + originalFileName);
		}

		String uniqueFileName = System.currentTimeMillis() + "_" + originalFileName.replaceAll("\\s+", "_");
		Path targetLocation = uploadPath.resolve(uniqueFileName);

		Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

		return uniqueFileName;
	}

	@Override
	public List<String> storeMultipleFiles(MultipartFile[] files) throws IOException {
		List<String> savedFiles = new ArrayList<>();
		if (files == null || files.length == 0)
			throw new IOException("No files provided!");
		for (MultipartFile file : files) {
			if (!file.isEmpty()) {
				String savedFilename = storeFile(file);
				savedFiles.add(savedFilename);
			}
		}
		if (savedFiles.isEmpty())
			throw new IOException("All files were empty!");
		return savedFiles;
	}

	@Transactional
	@Override
	public ManualInvoice updateUploadedFilesOnly(ManualInvoice invoice) {
		return invoiceRepository.save(invoice);
	}

	@Override
	public List<String> getAllTemplates() {
		Path dirPath = Paths.get(uploadDir);
		if (!Files.exists(dirPath))
			return List.of();

		try {
			return Files.list(dirPath).filter(Files::isRegularFile).map(p -> p.getFileName().toString())
					.collect(Collectors.toList());
		} catch (IOException e) {
			return List.of();
		}
	}

	@Override
	public Resource loadFileAsResource(String filename) throws Exception {
		File file = new File(uploadDir, filename);
		if (!file.exists())
			throw new FileNotFoundException("File not found: " + filename);
		return new UrlResource(file.toURI());
	}

	// commented by vasim
//	public Page<ManualInvoice> getAllInvoicesWithPaginationAndSearch(int page, int size, String sortField,
//			String sortDir, String keyword) {
//
//		// Fallback safety
//		if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
//			sortDir = "asc";
//		}
//
//		Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
//
//		Pageable pageable = PageRequest.of(page, size, sort);
//
//		Page<ManualInvoice> invoicePage = invoiceRepository.searchInvoices(keyword, pageable);
//
//		// Enrich invoice response
//		invoicePage.getContent().forEach(this::enrichFromVendorService);
//
//		return invoicePage;
//	}

	private void enrichFromVendorService(ManualInvoice invoice) {

		// Skip if customer name missing
		if (!StringUtils.hasText(invoice.getCustomer())) {
			return;
		}

		List<VendorDTO> vendors = vendorFeignClient.searchVendors(invoice.getCustomer());

		if (vendors == null || vendors.isEmpty()) {
			return;
		}

		VendorDTO vendor = vendors.get(0);

		// ✅ Set billing address if missing
		if (invoice.getBillingAddress() == null || invoice.getBillingAddress().getStreet() == null) {

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

		if (invoiceRepository.existsByPoNumberAndIdNot(request.getPoNumber(), invoice.getId())) {
			throw new RuntimeException("PO Number already exists");
		}

		invoice.setCustomer(request.getCustomer());
		invoice.setCustomerEmail(request.getCustomerEmail());
		invoice.setCustomerPhone(request.getCustomerPhone());
		invoice.setConsultantId(request.getConsultantId());
		invoice.setConsultantName(request.getConsultantName());

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

	@Override
	public Map<String, Long> getInvoiceCounts() {
		Map<String, Long> counts = new HashMap<>();
		counts.put("total", invoiceRepository.getTotalInvoiceCount());
		counts.put("paid", invoiceRepository.getPaidInvoiceCount());
		counts.put("pending", invoiceRepository.getPendingInvoiceCount());
		counts.put("OverDue", invoiceRepository.getOverdueInvoiceCount());
		return counts;
	}

	@Override
	public Long getTodayOverdueCount() {
		return invoiceRepository.countOverdueInvoicesForToday(LocalDate.now());
	}

	@Override
	public List<ManualInvoice> getTodayOverdueInvoices() {
		return invoiceRepository.findOverdueInvoicesForToday(LocalDate.now());
	}

	// vasim/03/03
	@Override
	public Page<ManualInvoice> getAllInvoicesWithPaginationAndSearch(int page, int size, String sortField,
			String sortDir, String keyword) {

		// ---------- SORT DIRECTION SAFETY ----------
		if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
			sortDir = "asc";
		}

		// ---------- SORT FIELD SAFETY (prevents crash) ----------
		if (sortField == null || sortField.isBlank()) {
			sortField = "createdAt"; // default column
		}

		Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();

		Pageable pageable = PageRequest.of(page, size, sort);

		// ---------- 🔥 KEYWORD FORMATTING (THIS WAS MISSING) ----------
		if (keyword == null || keyword.trim().isEmpty()) {
			keyword = null;
		} else {
			keyword = "%" + keyword.trim().toLowerCase() + "%";
		}

		// ---------- REPOSITORY CALL ----------
		Page<ManualInvoice> invoicePage = invoiceRepository.searchInvoices(keyword, pageable);

		// ---------- ENRICH RESPONSE ----------
		invoicePage.getContent().forEach(this::enrichFromVendorService);

		return invoicePage;
	}

	public void sendInvoiceMail(String invoiceNumber) {
		System.out.println("Sending invoice mail to: " + invoiceNumber);
		ManualInvoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
				.orElseThrow(() -> new RuntimeException("Invoice not found"));

		ConsultantDTO consultant = consultantFeignClient.getConsultant(invoice.getConsultantId());

		String email = consultant.getInvoiceMail();

		invoiceEmailService.sendInvoiceMail(email, invoice);
	}
}
