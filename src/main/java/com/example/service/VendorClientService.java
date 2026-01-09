package com.example.service;

import java.util.List;

import com.example.DTO.VendorDTO;

public interface VendorClientService {
	
	public List<VendorDTO> fetchVendorByName(String name);

}
