package com.mes.infra.dal.shared.algorithm.po;

import com.mes.domain.shared.algorithm.entity.AlgorithmCoreApiCallRecord;
import com.mes.infra.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "algorithmCoreApiCallRecord")
public class AlgorithmCoreApiCallRecordPo extends BasePO<AlgorithmCoreApiCallRecord> {

    private String mode;
    private String url;
    private String apiPath;
    private String requestBody;
    private String callbackCustomValue;
    private String type;

    @Override
    public AlgorithmCoreApiCallRecord toDO() {
        AlgorithmCoreApiCallRecord record = new AlgorithmCoreApiCallRecord();
        copyBaseFieldsToDO(record);
        record.setMode(this.mode);
        record.setUrl(this.url);
        record.setApiPath(this.apiPath);
        record.setRequestBody(this.requestBody);
        record.setCallbackCustomValue(this.callbackCustomValue);
        record.setType(this.type);
        return record;
    }

    @Override
    protected BasePO<AlgorithmCoreApiCallRecord> fromDO(AlgorithmCoreApiCallRecord record) {
        if (record == null) {
            return null;
        }
        this.mode = record.getMode();
        this.url = record.getUrl();
        this.apiPath = record.getApiPath();
        this.requestBody = record.getRequestBody();
        this.callbackCustomValue = record.getCallbackCustomValue();
        this.type = record.getType();
        return this;
    }
}
