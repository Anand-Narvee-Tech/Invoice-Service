//package com.example.config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Service;
//
//import com.example.entity.ManualInvoice;
//
//@Service
//public class InvoiceEmailServices {
//
//    @Autowired
//    private JavaMailSender mailSender;
//
//    public void sendInvoiceMail(String toMail, ManualInvoice invoice) {
//
//        try {
//
//            SimpleMailMessage message = new SimpleMailMessage();
//
//            message.setTo(toMail);
//            message.setSubject("Invoice Generated - " + invoice.getInvoiceNumber());
//
//            String body =
//                    "Hello,\n\n" +
//                    "A new invoice has been generated.\n\n" +
//                    "Invoice Number: " + invoice.getInvoiceNumber() + "\n" +
//                    "Customer: " + invoice.getCustomer() + "\n" +
//                    "Amount Due: " + invoice.getAmountDue() + " " + invoice.getCurrency() + "\n" +
//                    "Due Date: " + invoice.getDueDate() + "\n\n" +
//                    "Regards,\nNarvee ATS";
//
//            message.setText(body);
//
//            mailSender.send(message);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}