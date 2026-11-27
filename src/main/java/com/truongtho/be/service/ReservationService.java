package com.truongtho.be.service;

import com.truongtho.be.dto.request.ReservationRequest;
import com.truongtho.be.dto.response.ReservationResponse;
import com.truongtho.be.entity.DiningTable;
import com.truongtho.be.entity.Reservation;
import com.truongtho.be.enums.ReservationStatus;
import com.truongtho.be.enums.TableStatus;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.mapper.ReservationMapper;
import com.truongtho.be.repository.CustomerRepository;
import com.truongtho.be.repository.DiningTableRepository;
import com.truongtho.be.repository.EmployeeRepository;
import com.truongtho.be.repository.ReservationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationService {
    final ReservationRepository reservationRepository;
    final CustomerRepository customerRepository;
    final EmployeeRepository employeeRepository;
    final DiningTableRepository diningTableRepository;
    final ReservationMapper reservationMapper;
    private static final int DINING_MINUTES = 120;
    private static final Set<ReservationStatus> ACTIVE =
            Set.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.ARRIVED);





    @Transactional
    public ReservationResponse create(ReservationRequest reservationRequest, String userId, boolean byStaff){
        assertCapacity(reservationRequest.getReservationTime(), reservationRequest.getPartySize(), null);
        Reservation reservation = new Reservation();
        reservation.setReservationCode(generateCode());

        reservation.setContactName(reservationRequest.getContactName());
        reservation.setContactPhone(reservationRequest.getContactPhone());
        reservation.setReservationTime(reservationRequest.getReservationTime());
        reservation.setPartySize(reservationRequest.getPartySize());
        reservation.setNote(reservationRequest.getNote());        reservation.setReservationStatus(byStaff ? ReservationStatus.CONFIRMED : ReservationStatus.PENDING);
        if (userId != null) {
            customerRepository.findById(userId).ifPresent(reservation::setCustomer);
            if (byStaff) {
                employeeRepository.findById(userId).ifPresent(reservation::setConfirmBy);
            }
        }

        Reservation saveReservation = reservationRepository.save(reservation);

        return reservationMapper.toResponse(saveReservation);
    }
    private String generateCode(){
        String code;
        do{
            code = "RES" +   UUID.randomUUID().toString().substring(0,8).toUpperCase();
        }while (reservationRepository.existsByReservationCode(code));
        return code;
    }
    private void assertCapacity(LocalDateTime time, int partySize, Long excludeId) {
        if (!diningTableRepository.existsByTableStatusNotAndCapacityGreaterThanEqual(
                TableStatus.MAINTENANCE, partySize)) {
            throw new IllegalStateException(
                    "Nhà hàng chưa có bàn đủ chỗ cho " + partySize + " khách, vui lòng liên hệ trực tiếp");
        }
        long usable = diningTableRepository.countByTableStatusNot(TableStatus.MAINTENANCE);
        long busy = reservationRepository.countOverlapping(
                ACTIVE,
                time.minusMinutes(DINING_MINUTES),
                time.plusMinutes(DINING_MINUTES),
                excludeId == null ? 0L : excludeId);
        if (busy >= usable) {
            throw new IllegalStateException("Khung giờ này đã kín bàn, vui lòng chọn giờ khác");
        }
    }


    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id){
        Reservation reservation  = reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation not found" + id));

        return reservationMapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationRequest reservationRequest){
        Reservation reservation  = reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation not found" + id));

        if((reservation.getReservationStatus() != ReservationStatus.PENDING) && (reservation.getReservationStatus() != ReservationStatus.CONFIRMED )){
            throw new IllegalStateException("Chỉ có đặt bàn đang chờ hoặc đã chấp nhận mới được phép sửa");
        }
        assertCapacity(reservationRequest.getReservationTime(), reservationRequest.getPartySize(), id);
        reservation.setContactPhone(reservationRequest.getContactPhone());
        reservation.setContactName(reservationRequest.getContactName());
        reservation.setNote(reservationRequest.getNote());
        reservation.setPartySize(reservationRequest.getPartySize());
        reservation.setReservationTime(reservationRequest.getReservationTime());

        Reservation save = reservationRepository.save(reservation);
        return reservationMapper.toResponse(reservation);
    }


    private Reservation findOwned(Long id, String userId) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found" + id));
        if (r.getCustomer() == null || !r.getCustomer().getId().equals(userId)) {
            throw new ResourceNotFoundException("Reservation not found" + id);
        }
        return r;
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getMyReservations(String userId, Pageable pageable) {
        return reservationRepository.findByCustomer_Id(userId, pageable)
                .map(reservationMapper::toResponse);
    }

    @Transactional
    public ReservationResponse updateMine(Long id, ReservationRequest request, String userId) {
        Reservation r = findOwned(id, userId);
        ReservationResponse res = updateReservation(id, request);
        if (r.getReservationStatus() == ReservationStatus.CONFIRMED) {
            r.setReservationStatus(ReservationStatus.PENDING);
            r.setConfirmBy(null);
            return reservationMapper.toResponse(reservationRepository.save(r));
        }
        return res;
    }

    @Transactional
    public ReservationResponse cancelMine(Long id, String userId) {
        Reservation r = findOwned(id, userId);
        if (r.getReservationStatus() != ReservationStatus.PENDING
                && r.getReservationStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Chỉ có thể hủy đặt bàn đang chờ hoặc đã được chấp nhận");
        }
        return updateStatus(id, ReservationStatus.CANCELLED, null);
    }

    @Transactional
    public void deleteReservation(Long id){
        Reservation reservation  = reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reservation not found" + id));
        if((reservation.getReservationStatus() != ReservationStatus.PENDING) ){
            throw new IllegalStateException("Chỉ có đặt bàn đang chờ mới đươc xóa");
        }
        reservationRepository.delete(reservation);
    }


    @Transactional(readOnly = true)
    public Page<ReservationResponse> getAllReservations(Pageable pageable){
        return reservationRepository.findAll(pageable)
                .map(reservationMapper::toResponse);
    }

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED  = Map.of(
            ReservationStatus.PENDING,Set.of(ReservationStatus.CONFIRMED,ReservationStatus.CANCELLED),
            ReservationStatus.CONFIRMED,Set.of(ReservationStatus.ARRIVED,ReservationStatus.CANCELLED,ReservationStatus.NO_SHOW),
            ReservationStatus.ARRIVED,Set.of(ReservationStatus.COMPLETED,  ReservationStatus.CANCELLED)
    );

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatus next, Long tableId){
        Reservation reservation = reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("reservation not found "+ id));
        ReservationStatus current = reservation.getReservationStatus();
        if(!ALLOWED.getOrDefault(current,Set.of()).contains(next)){
            throw new IllegalStateException("Trạng thái yêu cầu đặt bàn không hợp lệ");
        }

        if (current == ReservationStatus.ARRIVED
                && (next == ReservationStatus.CANCELLED || next == ReservationStatus.COMPLETED)) {
            DiningTable seated = reservation.getDiningTable();
            if (seated != null) {
                if (next == ReservationStatus.CANCELLED && seated.getTableStatus() == TableStatus.SERVING) {
                    throw new IllegalStateException("Bàn đang có hóa đơn mở, hãy hủy hoặc thanh toán hóa đơn trước");
                }
                if (seated.getTableStatus() == TableStatus.RESERVED) {
                    seated.setTableStatus(TableStatus.AVAILABLE);
                }
            }
        }
        if (next == ReservationStatus.ARRIVED) {
            if (tableId == null) {
                throw new IllegalStateException("Vui lòng chọn bàn cho khách");
            }
            DiningTable table = diningTableRepository.findByIdForUpdate(tableId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn: " + tableId));
            if (table.getTableStatus() != TableStatus.AVAILABLE) {
                throw new IllegalStateException("Bàn không còn trống");
            }
            reservation.setDiningTable(table);
            table.setTableStatus(TableStatus.RESERVED);
        }
        reservation.setReservationStatus(next);
        reservationRepository.save(reservation);
        return reservationMapper.toResponse(reservation);

    }
}
