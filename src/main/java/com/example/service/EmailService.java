package com.example.service;

import com.example.DTO.UserDTO;
import com.example.entity.ManualInvoice;

public interface EmailService {
	
	public void sendOverdueInvoiceEmail(UserDTO user, ManualInvoice invoice);

}
