package com.example.service;

public interface InvoiceEmailService {
	
	 public void sendOverdueInvoiceEmail(String authHeader, String invoiceNumber) ;

}
