package com.example.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorDTO {

	    private String vendorName;
	    private String email;
	    private String phoneNumber;
	    private VendorAddressDTO vendorAddress;
}
