package com.example.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.DTO.VendorDTO;
import com.example.client.VendorFeignClient;
import com.example.service.VendorClientService;

@Service
public class VendorClientServiceImpl implements VendorClientService {

    @Autowired
    private VendorFeignClient vendorFeignClient;

    @Override
    public List<VendorDTO> fetchVendorByName(String name) {
        return vendorFeignClient.searchVendors(name);
    }

}

