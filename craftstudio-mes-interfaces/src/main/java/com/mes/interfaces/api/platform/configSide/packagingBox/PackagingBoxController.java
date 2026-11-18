package com.mes.interfaces.api.platform.configSide.packagingBox;

import com.mes.application.dto.req.packagingBox.PackagingBoxListRequest;
import com.mes.application.dto.req.packagingBox.PackagingBoxRequest;
import com.mes.application.dto.resp.PagedApiResponse;
import com.mes.application.dto.resp.packagingBox.PackagingBoxResponse;
import com.mes.domain.base.repository.ApiResponse;
import com.mes.domain.delivery.packagingBox.entity.PackagingBox;
import com.mes.domain.delivery.packagingBox.service.PackagingBoxService;
import com.piliofpala.craftstudio.shared.domain.base.repository.PagedResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/configSide/packagingBox")
public class PackagingBoxController {
    private final PackagingBoxService packagingBoxService;

    public PackagingBoxController(PackagingBoxService packagingBoxService) {
        this.packagingBoxService = packagingBoxService;
    }

    @PostMapping("/add")
    public ApiResponse<PackagingBoxResponse> add(@RequestBody PackagingBoxRequest request) {
        return ApiResponse.success(PackagingBoxResponse.from(packagingBoxService.add(request.toDomainEntity())));
    }

    @GetMapping("/{id}")
    public ApiResponse<PackagingBoxResponse> detail(@PathVariable("id") String id) {
        return ApiResponse.success(PackagingBoxResponse.from(packagingBoxService.findById(id)));
    }

    @PostMapping("/edit")
    public ApiResponse<String> edit(@RequestBody PackagingBoxRequest request) {
        packagingBoxService.update(request.toDomainEntity());
        return ApiResponse.success();
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<String> delete(@PathVariable("id") String id) {
        packagingBoxService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/list")
    public PagedApiResponse<PackagingBoxResponse> list(@RequestBody PackagingBoxListRequest request) {
        PagedResult<PackagingBox> result = packagingBoxService.list(
                request.getBoxName(), request.getCode(), request.getCardboardHeight(), request.getCardboardWidth(),
                request.getCurrent(), request.getSize());
        List<PackagingBoxResponse> items = result.items().stream().map(PackagingBoxResponse::from).toList();
        return PagedApiResponse.success(items, request.getCurrent(), request.getSize(), result.total());
    }

    @GetMapping("/cardboardHeights")
    public ApiResponse<List<BigDecimal>> listCardboardHeights() {
        return ApiResponse.success(packagingBoxService.listCardboardHeights());
    }

    @GetMapping("/cardboardWidths")
    public ApiResponse<List<BigDecimal>> listCardboardWidths() {
        return ApiResponse.success(packagingBoxService.listCardboardWidths());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handleUnreadableRequest() {
        return ApiResponse.fail(ApiResponse.RepStatusCode.badParams, "请求体不能为空且字段必须符合规定的 JSON 格式");
    }
}
