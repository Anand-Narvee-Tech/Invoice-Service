package com.example.service;

import com.example.entity.ManualInvoice;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ManualInvoiceService1 {
  ManualInvoice saveInvoice(ManualInvoice paramManualInvoice);
  public boolean isPoNumberDuplicate(String poNumber, Long invoiceId);
  
  ManualInvoice getInvoiceById(Long paramLong);
  
  ManualInvoice updateInvoice(Long paramLong, ManualInvoice paramManualInvoice);
  
  List<ManualInvoice> getAllInvoices();
  
  void deleteInvoice(Long paramLong);
  ManualInvoice updateManualInvoice(Long id, ManualInvoice invoice);
  
  String storeFile(MultipartFile paramMultipartFile) throws IOException;
  
  List<String> storeMultipleFiles(MultipartFile[] files) throws IOException;
  public ManualInvoice updateUploadedFilesOnly(ManualInvoice invoice, List<String> newFiles) ;
  
//  List<String> getFilesByInvoiceId(Long id) throws IOException;
  
  List<String> getAllTemplates(); 
  
  Resource loadFileAsResource(String paramString) throws Exception;
  
  Page<ManualInvoice> searchInvoices(String paramString, Pageable paramPageable);
  
  
}

