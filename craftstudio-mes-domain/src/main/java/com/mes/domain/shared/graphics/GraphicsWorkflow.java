package com.mes.domain.shared.graphics;

import java.util.Date;

/** Durable MES orchestration, separate from an individual imgUtil rendering task. */
public record GraphicsWorkflow(String id,String manufacturerMetaId,String kind,String businessId,String status,
                               String payload,String result,String error,Date createdAt,Date updatedAt,String owner) { }
