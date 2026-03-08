package com.example.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.entity.ManualInvoice;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;

@Service
public class InvoiceEmailService {

	@Autowired
	private JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String fromMail;

	public void sendInvoiceMail(String toMail, ManualInvoice invoice) {

		try {

			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);

			helper.setFrom(fromMail);
			helper.setTo(toMail);
			helper.setSubject("Invoice Generated - " + invoice.getInvoiceNumber());

			String body = "<html>"
					+ "<body style='margin:0;padding:0;background:#f0f4f8;font-family:Arial,sans-serif;'>" +

					"<table width='100%' cellpadding='0' cellspacing='0'>" + "<tr><td align='center'>" +

					"<table width='620' cellpadding='0' cellspacing='0' style='background:#ffffff;margin-top:40px;border-radius:10px;overflow:hidden;box-shadow:0 6px 20px rgba(0,0,0,0.15);'>"
					+

					"<tr>"
					+ "<td style='background:linear-gradient(90deg,#6a11cb,#2575fc);padding:30px;text-align:center;color:white;'>"
					+ "<h1 style='margin:0;'>Narvee</h1>" + "<p style='margin-top:5px;'>Invoice Notification</p>"
					+ "</td>" + "</tr>" +

					"<tr>" + "<td style='padding:30px;color:#333;font-size:15px;'>" +

					"<p>Hello <b style='color:#2575fc'>" + invoice.getCustomer() + "</b>,</p>" +

					"<p>Your invoice has been generated successfully.</p>" +

					"<table width='100%' cellpadding='12' style='border-collapse:collapse;margin-top:20px;font-size:14px'>"
					+

					"<tr style='background:#f5f7fa'>" + "<td><b>Invoice Number</b></td>" + "<td>"
					+ invoice.getInvoiceNumber() + "</td>" + "</tr>" +

					"<tr>" + "<td><b>Invoice Date</b></td>" + "<td>" + invoice.getInvoiceDate() + "</td>" + "</tr>" +

					"<tr style='background:#f5f7fa'>" + "<td><b>Due Date</b></td>" + "<td>" + invoice.getDueDate()
					+ "</td>" + "</tr>" +

					"<tr>" + "<td><b>Consultant</b></td>" + "<td>" + invoice.getConsultantName() + "</td>" + "</tr>" +

					"<tr style='background:#f5f7fa'>" + "<td><b>Total Amount</b></td>"
					+ "<td style='color:#27ae60;font-weight:bold'>" + invoice.getTotal() + " " + invoice.getCurrency()
					+ "</td>" + "</tr>" +

					"<tr>" + "<td><b>Amount Due</b></td>" + "<td style='color:#e74c3c;font-weight:bold'>"
					+ invoice.getAmountDue() + "</td>" + "</tr>" +

					"</table>" +

					"<div style='margin-top:30px;text-align:center'>"
					+ "<a href='#' style='background:#2575fc;color:white;padding:12px 25px;text-decoration:none;border-radius:5px;font-weight:bold'>View Invoice</a>"
					+ "</div>" +

					"<p style='margin-top:30px'>Regards,<br><b>Narvee Team</b></p>" +

					"</td>" + "</tr>" +

					"<tr>" + "<td style='background:#2c3e50;color:white;text-align:center;padding:15px;font-size:12px'>"
					+ "© 2026 Narvee ATS • All Rights Reserved<br>" + "no-reply@narvee.com" + "</td>" + "</tr>" +

					"</table>" +

					"</td></tr></table>" +

					"</body>" + "</html>";
			helper.setText(body, true);

			mailSender.send(message);

			System.out.println("Invoice mail sent successfully to: " + toMail);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}