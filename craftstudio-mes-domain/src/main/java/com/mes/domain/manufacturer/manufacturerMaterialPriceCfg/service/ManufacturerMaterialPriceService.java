package com.mes.domain.manufacturer.manufacturerMaterialPriceCfg.service;

import com.mes.domain.manufacturer.manufacturerMaterialPriceCfg.repository.ManufacturerMaterialPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ManufacturerMaterialPriceService {

    @Autowired
    private ManufacturerMaterialPriceRepository manufacturerMaterialPriceRepository;


}
