package com.truongtho.be.service;

import com.truongtho.be.dto.request.InvoiceDetailRequest;
import com.truongtho.be.dto.response.InvoiceResponse;
import com.truongtho.be.entity.*;
import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.enums.InvoiceStatus;
import com.truongtho.be.enums.ProductStatus;
import com.truongtho.be.enums.TableStatus;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.mapper.InvoiceMapper;
import com.truongtho.be.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoiceService {

    InvoiceRepository invoiceRepository;
    ProductRepository productRepository;
    DiningTableRepository diningTableRepository;
    EmployeeRepository employeeRepository;
    InvoiceMapper invoiceMapper;

    @Transactional
    public InvoiceResponse createInvoice(Long tableId, String employeeId) {
        DiningTable table = diningTableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn: " + tableId));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên: " + employeeId));

        if (invoiceRepository.existsByDiningTable_IdAndInvoiceStatus(tableId, InvoiceStatus.PENDING)) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Bàn này đã có hóa đơn đang mở");
        }
        if (table.getTableStatus() != TableStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Bàn không ở trạng thái trống");
        }

        Invoice invoice = new Invoice();
        invoice.setDiningTable(table);
        invoice.setEmployee(employee);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setInvoiceStatus(InvoiceStatus.PENDING);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ZERO);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        table.setTableStatus(TableStatus.SERVING);
        return invoiceMapper.toResponse(savedInvoice);
    }

    @Transactional
    public InvoiceResponse addDetail(String invoiceId, InvoiceDetailRequest request) {
        Invoice invoice = getPendingInvoice(invoiceId);

        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Số lượng phải lớn hơn 0");
        }
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm: " + request.getProductId()));

        if (product.getProductStatus() == ProductStatus.OUT_OF_STOCK
                || product.getProductStatus() == ProductStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Sản phẩm hiện không thể bán");
        }
        if (product.getUnitPrice() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Sản phẩm chưa có giá");
        }

        InvoiceDetail detail = findDetail(invoice, product.getId());
        if (detail == null) {
            detail = new InvoiceDetail();
            detail.setInvoiceDetailId(new InvoiceDetailId(invoice.getId(), product.getId()));
            detail.setInvoice(invoice);
            detail.setProduct(product);
            detail.setQuantity(request.getQuantity());
            detail.setUnitPrice(product.getUnitPrice());
            invoice.getInvoiceDetails().add(detail);
        } else {
            detail.setQuantity(detail.getQuantity() + request.getQuantity());
        }
        detail.setTotal(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())));

        recalculateTotal(invoice);
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse updateDetail(String invoiceId, Long productId, Integer quantity) {
        Invoice invoice = getPendingInvoice(invoiceId);

        if (quantity == null || quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Số lượng phải lớn hơn 0");
        }
        InvoiceDetail detail = findDetail(invoice, productId);
        if (detail == null) {
            throw new ResourceNotFoundException("Không tìm thấy chi tiết hóa đơn");
        }
        detail.setQuantity(quantity);
        detail.setTotal(detail.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));

        recalculateTotal(invoice);
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse removeDetail(String invoiceId, Long productId) {
        Invoice invoice = getPendingInvoice(invoiceId);

        InvoiceDetail detail = findDetail(invoice, productId);
        if (detail == null) {
            throw new ResourceNotFoundException("Không tìm thấy chi tiết hóa đơn");
        }
        invoice.getInvoiceDetails().remove(detail);

        recalculateTotal(invoice);
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + invoiceId));
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public void cancelInvoice(String invoiceId) {
        Invoice invoice = getPendingInvoice(invoiceId);
        invoice.setInvoiceStatus(InvoiceStatus.CANCELLED);

        DiningTable table = invoice.getDiningTable();
        if (table != null && table.getTableStatus() == TableStatus.SERVING) {
            table.setTableStatus(TableStatus.AVAILABLE);
        }
    }

    private Invoice getPendingInvoice(String invoiceId) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + invoiceId));

        if (invoice.getInvoiceStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE, "Chỉ được thao tác với hóa đơn đang mở");
        }
        return invoice;
    }

    private InvoiceDetail findDetail(Invoice invoice, Long productId) {
        return invoice.getInvoiceDetails().stream()
                .filter(d -> d.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private void recalculateTotal(Invoice invoice) {
        BigDecimal subtotal = invoice.getInvoiceDetails().stream()
                .map(InvoiceDetail::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = invoice.getDiscountAmount() == null
                ? BigDecimal.ZERO
                : invoice.getDiscountAmount();

        invoice.setTotalAmount(subtotal.subtract(discount).max(BigDecimal.ZERO));
    }



    @Transactional(readOnly = true)
    public Page<InvoiceResponse> search(Long tableId, InvoiceStatus status, Pageable pageable) {
        return invoiceRepository.search(tableId, status, pageable)
                .map(invoiceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getOpenInvoiceByTable(Long tableId) {
        Invoice invoice = invoiceRepository
                .findFirstByDiningTable_IdAndInvoiceStatusOrderByIssuedAtDesc(tableId, InvoiceStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Bàn này chưa có hóa đơn đang mở"));
        return invoiceMapper.toResponse(invoice);
    }
}