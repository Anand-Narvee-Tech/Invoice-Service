package com.example.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.DTO.VendorDTO;

@FeignClient(name = "customer-service", url = "${customer.service.url}")
public interface VendorFeignClient {

    @GetMapping("/vendor/by-name")
    List<VendorDTO> searchVendors(@RequestParam("name") String name);
}
