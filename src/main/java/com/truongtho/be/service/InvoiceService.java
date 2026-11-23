package com.truongtho.be.service;

import com.truongtho.be.dto.request.InvoiceDetailRequest;
import com.truongtho.be.dto.response.InvoiceResponse;
import com.truongtho.be.entity.*;
import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.enums.InvoiceStatus;
import com.truongtho.be.enums.PaymentStatus;
import com.truongtho.be.enums.ProductStatus;
import com.truongtho.be.enums.TableStatus;
import com.truongtho.be.exception.BusinessException;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.mapper.InvoiceMapper;
import com.truongtho.be.repository.*;
import com.truongtho.be.enums.ReservationStatus;
import java.util.List;
import lombok.AccessLevel;
import java.util.ArrayList;
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
    ReservationRepository reservationRepository;
    ProductRepository productRepository;
    DiningTableRepository diningTableRepository;
    EmployeeRepository employeeRepository;
    InvoiceMapper invoiceMapper;
    CustomerRepository customerRepository;
    LoyaltyService loyaltyService;
    VoucherService voucherService;
    PaymentRepository paymentRepository;

    @Transactional
    public InvoiceResponse createInvoice(Long tableId, String employeeId) {
        DiningTable table = diningTableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn: " + tableId));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên: " + employeeId));

        if (invoiceRepository.existsByDiningTable_IdAndInvoiceStatus(tableId, InvoiceStatus.PENDING)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bàn này đã có hóa đơn đang mở");
        }
        if (table.getTableStatus() != TableStatus.AVAILABLE
                && table.getTableStatus() != TableStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bàn không ở trạng thái trống");
        }
        boolean wasReserved = table.getTableStatus() == TableStatus.RESERVED;

        Invoice invoice = new Invoice();
        invoice.setDiningTable(table);
        invoice.setEmployee(employee);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setInvoiceStatus(InvoiceStatus.PENDING);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(BigDecimal.ZERO);
        if (wasReserved) {
            reservationRepository
                    .findFirstByDiningTable_IdAndReservationStatusOrderByReservationTimeDesc(
                            tableId, ReservationStatus.ARRIVED)
                    .ifPresent(r -> {
                        invoice.setReservation(r);
                        if (r.getCustomer() != null) {
                            invoice.setCustomer(r.getCustomer());   // booked online -> points go to that account
                        }
                    });
        }




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
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Sản phẩm hiện không thể bán");
        }
        if (product.getUnitPrice() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Sản phẩm chưa có giá");
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
        loyaltyService.refund(invoice);
        invoice.setInvoiceStatus(InvoiceStatus.CANCELLED);

        DiningTable table = invoice.getDiningTable();
        if (table != null && table.getTableStatus() == TableStatus.SERVING) {
            table.setTableStatus(TableStatus.AVAILABLE);
        }
    }


    @Transactional
    public InvoiceResponse attachCustomer(String invoiceId, String keyword) {
        Invoice invoice = getPendingInvoice(invoiceId);
        String k = keyword.trim();
        Customer customer = customerRepository.findByUsers_EmailIgnoreCase(k)
                .or(() -> customerRepository.findByUsers_PhoneNumber(k))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng: " + k));

        if (invoice.getCustomer() != null && !invoice.getCustomer().getId().equals(customer.getId())) {
            loyaltyService.refund(invoice);          // points go back to the previous customer
            recalculateTotal(invoice);
        }
        invoice.setCustomer(customer);
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse detachCustomer(String invoiceId) {
        Invoice invoice = getPendingInvoice(invoiceId);
        loyaltyService.refund(invoice);
        invoice.setCustomer(null);
        recalculateTotal(invoice);
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse redeemPoints(String invoiceId, int points) {
        Invoice invoice = getPendingInvoice(invoiceId);
        loyaltyService.refund(invoice);              // start clean so re-applying never double-counts
        BigDecimal subtotal = subtotal(invoice);
        BigDecimal base = subtotal.subtract(voucherService.calculateDiscount(invoice.getVoucher(), subtotal));
        loyaltyService.redeem(invoice, points, base);
        recalculateTotal(invoice);
        return invoiceMapper.toResponse(invoice);
    }


    @Transactional
    public InvoiceResponse applyVoucher(String invoiceId, String code) {
        Invoice invoice = getPendingInvoice(invoiceId);
        invoice.setVoucher(voucherService.findUsable(code, subtotal(invoice)));
        recalculateTotal(invoice);
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse removeVoucher(String invoiceId) {
        Invoice invoice = getPendingInvoice(invoiceId);
        invoice.setVoucher(null);
        recalculateTotal(invoice);
        return invoiceMapper.toResponse(invoice);
    }

    private BigDecimal subtotal(Invoice invoice) {
        return invoice.getInvoiceDetails().stream()
                .map(InvoiceDetail::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Invoice getPendingInvoice(String invoiceId) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn: " + invoiceId));

        if (invoice.getInvoiceStatus() != InvoiceStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Chỉ được thao tác với hóa đơn đang mở");
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
        BigDecimal subtotal = subtotal(invoice);

        Voucher voucher = invoice.getVoucher();
        if (voucher != null && voucherService.isBelowMinimum(voucher, subtotal)) {
            invoice.setVoucher(null);                    // bill shrank below the voucher's minimum
            voucher = null;
        }
        BigDecimal voucherDiscount = voucherService.calculateDiscount(voucher, subtotal);

        int points = invoice.getPointsRedeemed() == null ? 0 : invoice.getPointsRedeemed();
        if (points > loyaltyService.maxRedeemablePoints(subtotal.subtract(voucherDiscount))) {
            loyaltyService.refund(invoice);              // items/voucher changed: points no longer allowed
            points = 0;
        }

        BigDecimal discount = voucherDiscount.add(loyaltyService.pointsValue(points)).min(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        BigDecimal before = invoice.getTotalAmount();
        invoice.setDiscountAmount(discount);
        invoice.setTotalAmount(total);

        // An unpaid QR/payment was created for the OLD amount -> void it so it can't settle the wrong sum.
        if (invoice.getId() != null && (before == null || before.compareTo(total) != 0)) {
            paymentRepository.findAllByInvoice_IdAndPaymentStatus(invoice.getId(), PaymentStatus.PENDING)
                    .forEach(p -> p.setPaymentStatus(PaymentStatus.CANCELLED));
        }
    }



    @Transactional(readOnly = true)
    public List<InvoiceResponse> findMyOpenInvoices(String employeeId) {
        return invoiceRepository
                .findByEmployee_IdAndInvoiceStatusOrderByIssuedAtDesc(employeeId, InvoiceStatus.PENDING)
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
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


    @Transactional
    public InvoiceResponse moveTable(String invoiceId, Long toTableId) {
        Invoice invoice = getPendingInvoice(invoiceId);
        DiningTable from = invoice.getDiningTable();
        if (from != null && from.getId().equals(toTableId)) {
            throw new IllegalStateException("Hóa đơn đang ở bàn này rồi");
        }
        DiningTable to = diningTableRepository.findByIdForUpdate(toTableId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn: " + toTableId));
        if (to.getTableStatus() != TableStatus.AVAILABLE
                || invoiceRepository.existsByDiningTable_IdAndInvoiceStatus(toTableId, InvoiceStatus.PENDING)) {
            throw new IllegalStateException("Bàn đích không trống");
        }

        to.setTableStatus(TableStatus.SERVING);
        if (from != null && from.getTableStatus() == TableStatus.SERVING) {
            from.setTableStatus(TableStatus.AVAILABLE);
        }
        invoice.setDiningTable(to);

        Reservation r = invoice.getReservation();
        if (r != null && r.getReservationStatus() == com.truongtho.be.enums.ReservationStatus.ARRIVED) {
            r.setDiningTable(to);
        }
        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse mergeTable(String invoiceId, Long fromTableId) {
        Invoice target = getPendingInvoice(invoiceId);
        DiningTable targetTable = target.getDiningTable();
        if (targetTable != null && targetTable.getId().equals(fromTableId)) {
            throw new IllegalStateException("Không thể gộp một bàn với chính nó");
        }
        Invoice found = invoiceRepository
                .findFirstByDiningTable_IdAndInvoiceStatusOrderByIssuedAtDesc(fromTableId, InvoiceStatus.PENDING)
                .orElseThrow(() -> new IllegalStateException("Bàn cần gộp không có hóa đơn đang mở"));
        Invoice source = getPendingInvoice(found.getId());

        for (InvoiceDetail sd : new ArrayList<>(source.getInvoiceDetails())) {
            Product p = sd.getProduct();
            InvoiceDetail td = findDetail(target, p.getId());
            if (td == null) {
                td = new InvoiceDetail();
                td.setInvoiceDetailId(new InvoiceDetailId(target.getId(), p.getId()));
                td.setInvoice(target);
                td.setProduct(p);
                td.setQuantity(sd.getQuantity());
                td.setUnitPrice(sd.getUnitPrice());
                target.getInvoiceDetails().add(td);
            } else {
                td.setQuantity(td.getQuantity() + sd.getQuantity());
            }
            td.setTotal(td.getUnitPrice().multiply(BigDecimal.valueOf(td.getQuantity())));
        }

        loyaltyService.refund(source);
        if (target.getCustomer() == null && source.getCustomer() != null) {
            target.setCustomer(source.getCustomer());
        }
        source.getInvoiceDetails().clear();
        source.setInvoiceStatus(InvoiceStatus.CANCELLED);
        source.setVoucher(null);
        source.setDiscountAmount(BigDecimal.ZERO);
        source.setTotalAmount(BigDecimal.ZERO);
        DiningTable sourceTable = source.getDiningTable();
        if (sourceTable != null && sourceTable.getTableStatus() == TableStatus.SERVING) {
            sourceTable.setTableStatus(TableStatus.AVAILABLE);
        }

        recalculateTotal(target);
        return invoiceMapper.toResponse(target);
    }
}