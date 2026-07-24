package com.piliofpala.craftstudio.pangolin.infra.gatherplatform.wdt;

import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.platform.vo.GatherPlatformType;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.entity.ProductSpec;
import com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.exception.BrandNotExistException;
import com.piliofpala.craftstudio.pangolin.infra.gatherplatform.GatherPlatform;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ProductSpecTest {
    @Test
    @Disabled("影响真实数据，打包不予执行")
    void pushProductSpec() {
        ProductSpec productSpec = new ProductSpec(
                "马国臣","CODE-2",
                "https://craftstudio-ordering-test.oss-cn-hangzhou.aliyuncs.com/asset/org/2050484765549539330/6a3d1f7d2dc9301cc83cc47d/filePreview/preview.png",
                "测试品牌122"
        );
        try{
            GatherPlatform.getInstance(GatherPlatformType.WDT).pushProductSpec(productSpec);
        }catch (BrandNotExistException brandNotExistException){
            brandNotExistException.printStackTrace();
        }
    }

    @Test
    void queryProductSpecByCode(){
        var productSpec = GatherPlatform.getInstance(GatherPlatformType.WDT).queryProductSpecByCode("CODE-2");
        System.out.println(productSpec);
    }
}
