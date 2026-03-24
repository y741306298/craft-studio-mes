package com.mes.application.command.manufacturerMeta;

import com.mes.domain.base.UnitPrice;
import com.mes.domain.manufacturer.enums.CfgStatus;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.entity.ManufacturerMtsProductCfg;
import com.mes.domain.manufacturer.manufacturerMtsProductCfg.vo.ManufacturerMtsProductSpec;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.entity.ManufacturerProcessPriceCfg;
import com.mes.domain.manufacturer.manufacturerProcessPriceCfg.vo.MaterialProcessPrice;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlow;
import com.mes.domain.manufacturer.procedureFlow.entity.ProcedureFlowNode;
import com.mes.domain.manufacturer.procedureFlow.enums.FlowStatus;
import com.mes.domain.manufacturer.procedureFlow.enums.NodeStatus;
import com.mes.domain.shared.enums.ProductUnit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MockExternalApiService {

    private static final String[] PRODUCT_NAMES = {"T 恤衫", "衬衫", "卫衣", "POLO 衫", "运动服", "休闲裤", "牛仔裤", "外套", "风衣", "毛衣"};
    private static final String[] PROCESS_NAMES = {"裁剪", "缝制", "整烫", "包装", "质检", "印花", "绣花", "水洗", "打磨", "染色"};
    private static final String[] MATERIALS = {"纯棉", "涤纶", "棉麻混纺", "真丝", "羊毛", "牛仔布", "针织面料", "雪纺", "皮革", "羽绒"};

    public List<ManufacturerMtsProductCfg> getManufacturerMtsProductsByTempId(String manufacturerTempId) {
        List<ManufacturerMtsProductCfg> products = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            ManufacturerMtsProductCfg product = new ManufacturerMtsProductCfg();
            product.setProductId("PROD_" + manufacturerTempId + "_" + String.format("%03d", i + 1));
            product.setProductName(PRODUCT_NAMES[i]);
            product.setProductPreviewUrl("https://example.com/preview/" + product.getProductId() + ".jpg");
            product.setStatus(CfgStatus.NORMAL);

            List<ManufacturerMtsProductSpec> specs = new ArrayList<>();
            String[] sizes = {"XS", "S", "M", "L", "XL", "XXL"};
            String[] colors = {"白色", "黑色", "灰色", "蓝色", "红色"};

            for (String size : sizes) {
                for (String color : colors) {
                    ManufacturerMtsProductSpec spec = new ManufacturerMtsProductSpec();
                    spec.setId("SPEC_" + product.getProductId() + "_" + size + "_" + color);
                    spec.setName(size + " " + color);
                    spec.setPreviewUrl("https://example.com/spec/" + spec.getId() + ".jpg");
                    
                    List<String> materials = new ArrayList<>();
                    materials.add(MATERIALS[i]);
                    spec.setMaterials(materials);
                    
                    List<ProcedureFlow> flows = new ArrayList<>();
                    ProcedureFlow flow = createMockProcedureFlow(i);
                    flows.add(flow);
                    spec.setProcedureFlow(flows);
                    
                    spec.setCustomizable(true);
                    
                    UnitPrice price = new UnitPrice();
                    price.setPrice(Double.valueOf(random.nextInt(500) + 50));
                    price.setUnit(ProductUnit.PIECE.getSymbol());
                    spec.setPrice(price);
                    
                    spec.setStatus(CfgStatus.NORMAL);
                    
                    specs.add(spec);
                }
            }
            product.setMtsProductSpecs(specs);

            products.add(product);
        }

        return products;
    }

    private ProcedureFlow createMockProcedureFlow(int index) {
        ProcedureFlow flow = new ProcedureFlow();
        flow.setProcedureFlowId("FLOW_" + String.format("%03d", index + 1));
        flow.setProcedureFlowName(PROCESS_NAMES[index] + "流程");
        flow.setFlowDescription(PROCESS_NAMES[index] + "标准工序流程");
        flow.setFlowStatus(FlowStatus.RUNNING);
        
        List<ProcedureFlowNode> nodes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ProcedureFlowNode node = new ProcedureFlowNode();
            node.setNodeId("NODE_" + String.format("%03d", index + 1) + "_" + i);
            node.setNodeName(PROCESS_NAMES[index] + "_步骤" + (i + 1));
            node.setProcedureId(flow.getProcedureFlowId());
            node.setNodeOrder(i + 1);
            node.setNodeStatus(i == 0 ? NodeStatus.COMPLETED : (i == 1 ? NodeStatus.ACTIVE : NodeStatus.PENDING));
            node.setNodeType("PROCESS");
            node.setDescription("步骤" + (i + 1) + "详细说明");
            node.setRemarks("节点备注信息");
            nodes.add(node);
        }
        flow.setNodes(nodes);
        flow.setTotalNodes(nodes.size());
        
        return flow;
    }

    public List<ManufacturerProcessPriceCfg> getManufacturerProcessPricesByTempId(String manufacturerTempId) {
        List<ManufacturerProcessPriceCfg> processes = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            ManufacturerProcessPriceCfg process = new ManufacturerProcessPriceCfg();
            process.setProcessId("PROC_" + manufacturerTempId + "_" + String.format("%03d", i + 1));
            process.setProcessName(PROCESS_NAMES[i]);
            process.setProcessCode("CODE_" + String.format("%03d", i + 1));
            process.setProcessDescription(PROCESS_NAMES[i] + "工艺描述");
            process.setProcessType("STANDARD");
            process.setCapacity(String.valueOf(random.nextInt(1000) + 100));
            process.setUnit("件/天");
            
            Double basePrice = Double.valueOf(random.nextInt(200) + 10);
            process.setBasePrice(basePrice);
            
            UnitPrice processPrice = new UnitPrice();
            processPrice.setPrice(basePrice);
            processPrice.setUnit(ProductUnit.PIECE.getSymbol());
            process.setProcessPrice(processPrice);
            
            process.setStatus(CfgStatus.NORMAL);
            process.setMaterials(MATERIALS[i]);

            ProcedureFlow procedureFlow = createMockProcedureFlow(i);
            process.setProcedureFlow(procedureFlow);

            List<MaterialProcessPrice> materialPrices = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                MaterialProcessPrice materialPrice = new MaterialProcessPrice();
                materialPrice.setMaterialId("MAT_" + String.format("%03d", (i + j) % 10));
                materialPrice.setMaterialName(MATERIALS[(i + j) % 10]);
                
                UnitPrice price = new UnitPrice();
                price.setPrice(Double.valueOf(random.nextInt(100) + 5));
                price.setUnit(ProductUnit.PIECE.getSymbol());
                materialPrice.setProcessPrice(price);
                
                materialPrice.setBasePrice(Double.valueOf(random.nextInt(100) + 5));
                
                materialPrices.add(materialPrice);
            }
            process.setMaterialProcessPrices(materialPrices);

            processes.add(process);
        }

        return processes;
    }
}
