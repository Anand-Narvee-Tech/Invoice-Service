package com.example.serviceImpl;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.example.DTO.UserDTO;
import com.example.constant.EmailSignatureConstants;
import com.example.entity.ManualInvoice;
import com.example.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService{

    private final JavaMailSender mailSender;

    @Override
    public void sendOverdueInvoiceEmail(UserDTO sender, ManualInvoice invoice) {

        SimpleMailMessage message = new SimpleMailMessage();

        // ✅ TO = customer
        message.setTo(invoice.getCustomerEmail());

        // ✅ FROM = logged-in user (optional, but recommended)
        message.setFrom(sender.getEmail());

        message.setSubject("Overdue Invoice: " + invoice.getInvoiceNumber());

        message.setText(
        	    "Hi " + invoice.getSalesRep() + ",\n\n" +
        	    "This is a reminder that invoice " + invoice.getInvoiceNumber() +
        	    " dated " + invoice.getInvoiceDate() +
        	    " is overdue. Please make the payment as soon as possible.\n\n" +
        	    "Amount Due: " + invoice.getAmountDue() + "\n" +
        	    "Due Date: " + invoice.getDueDate() + "\n\n" +

        	    "Regards,\n" +
        	    sender.getFullName() + "\n" +
        	    sender.getRoleName() + "\n" +
        	    sender.getCompanyName() + "\n" +
        	    EmailSignatureConstants.ADDRESS + "\n" +
        	    sender.getEmail() + "\n" +
        	    EmailSignatureConstants.WEBSITE + "\n" +
        	    EmailSignatureConstants.TAGLINE +
        	    EmailSignatureConstants.DISCLAIMER
        	);


        mailSender.send(message);

        log.info(
            "Overdue invoice email sent. Invoice={}, From={}, To={}",
            invoice.getInvoiceNumber(),
            sender.getEmail(),
            invoice.getCustomerEmail()
        );
    }

}
