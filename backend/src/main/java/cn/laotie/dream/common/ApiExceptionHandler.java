package cn.laotie.dream.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import java.util.Map;

/**
 * 统一错误响应，不向客户端泄漏 SQL、堆栈或账户内部字段。
 * @author 沙里飞
 * @email 1499058156@qq.com
 * @company 青海老铁软件有限公司
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private ResponseEntity<Map<String, Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(Api.map("code", status, "message", message));
    }
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> business(ApiException e) { return error(e.getStatus(), e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        return error(400, e.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> invalid(Exception e) { return error(400, "输入格式不正确，请检查后重试"); }
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> duplicate(Exception e) { return error(409, "记录已经存在，请刷新后重试"); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String,Object>> oversized(Exception e) { return error(413,"文件过大，图片最多 10 MB、视频最多 100 MB"); }
    @ExceptionHandler({MultipartException.class,MissingServletRequestPartException.class,MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String,Object>> multipart(Exception e) { return error(400,"请选择完整的图片或视频文件后重试"); }
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> missing(Exception e) { return error(404, "接口不存在"); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        log.error("Request failed", e);
        return error(500, "服务暂时不可用，请稍后重试");
    }
}
