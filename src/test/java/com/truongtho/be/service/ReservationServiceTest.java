package com.truongtho.be.service;

import com.truongtho.be.dto.request.ReservationRequest;
import com.truongtho.be.entity.Customer;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock CustomerRepository customerRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock DiningTableRepository diningTableRepository;
    @Mock ReservationMapper reservationMapper;
    @InjectMocks ReservationService service;


    private ReservationRequest request() {
        ReservationRequest r = new ReservationRequest();
        r.setContactName("An");
        r.setContactPhone("0901234567");
        r.setReservationTime(LocalDateTime.now().plusDays(1));
        r.setPartySize(4);
        return r;
    }

    private Reservation reservation(Long id, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setReservationStatus(status);
        return r;
    }

    private DiningTable table(Long id, TableStatus status) {
        DiningTable t = new DiningTable();
        t.setId(id);
        t.setTableStatus(status);
        return t;
    }

    private void found(Reservation r) {
        when(reservationRepository.findById(r.getId())).thenReturn(Optional.of(r));
    }

    private void capacity(boolean hasBigEnoughTable, long usableTables, long busy) {
        when(diningTableRepository.existsByTableStatusNotAndCapacityGreaterThanEqual(
                eq(TableStatus.MAINTENANCE), anyInt())).thenReturn(hasBigEnoughTable);
        if (hasBigEnoughTable) {
            when(diningTableRepository.countByTableStatusNot(TableStatus.MAINTENANCE)).thenReturn(usableTables);
            when(reservationRepository.countOverlapping(any(), any(), any(), anyLong())).thenReturn(busy);
        }
    }


    @Test
    void create_byCustomer_isPending() {
        capacity(true, 3, 0);
        when(reservationRepository.existsByReservationCode(anyString())).thenReturn(false);
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(request(), null, false);

        ArgumentCaptor<Reservation> saved = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(saved.capture());
        assertThat(saved.getValue().getReservationStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(saved.getValue().getReservationCode()).startsWith("RES");
    }

    @Test
    void create_byStaff_isConfirmed() {
        capacity(true, 3, 0);
        when(reservationRepository.existsByReservationCode(anyString())).thenReturn(false);
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(request(), null, true);

        ArgumentCaptor<Reservation> saved = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(saved.capture());
        assertThat(saved.getValue().getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void create_fullyBooked_throwsAndDoesNotSave() {
        capacity(true, 3, 3); // 3 bàn, 3 đơn trùng giờ

        assertThatThrownBy(() -> service.create(request(), null, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kín bàn");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void create_noTableBigEnough_throws() {
        capacity(false, 0, 0);

        assertThatThrownBy(() -> service.create(request(), null, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chưa có bàn đủ chỗ");
    }


    @Test
    void updateStatus_invalidTransition_throws() {
        Reservation r = reservation(1L, ReservationStatus.PENDING);
        found(r);

        assertThatThrownBy(() -> service.updateStatus(1L, ReservationStatus.ARRIVED, 5L))
                .isInstanceOf(IllegalStateException.class); // PENDING không được nhảy thẳng sang ARRIVED
    }

    @Test
    void updateStatus_finalStatus_cannotChange() {
        Reservation r = reservation(1L, ReservationStatus.COMPLETED);
        found(r);

        assertThatThrownBy(() -> service.updateStatus(1L, ReservationStatus.CANCELLED, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateStatus_arrived_withoutTable_throws() {
        found(reservation(1L, ReservationStatus.CONFIRMED));

        assertThatThrownBy(() -> service.updateStatus(1L, ReservationStatus.ARRIVED, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chọn bàn");
    }

    @Test
    void updateStatus_arrived_tableNotAvailable_throws() {
        found(reservation(1L, ReservationStatus.CONFIRMED));
        when(diningTableRepository.findByIdForUpdate(5L))
                .thenReturn(Optional.of(table(5L, TableStatus.SERVING)));

        assertThatThrownBy(() -> service.updateStatus(1L, ReservationStatus.ARRIVED, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không còn trống");
    }

    @Test
    void updateStatus_arrived_marksTableReserved() {
        Reservation r = reservation(1L, ReservationStatus.CONFIRMED);
        DiningTable t = table(5L, TableStatus.AVAILABLE);
        found(r);
        when(diningTableRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(t));

        service.updateStatus(1L, ReservationStatus.ARRIVED, 5L);

        assertThat(r.getReservationStatus()).isEqualTo(ReservationStatus.ARRIVED);
        assertThat(r.getDiningTable()).isSameAs(t);
        assertThat(t.getTableStatus()).isEqualTo(TableStatus.RESERVED);
    }

    @Test
    void updateStatus_cancelArrived_whileTableServing_throws() {
        Reservation r = reservation(1L, ReservationStatus.ARRIVED);
        r.setDiningTable(table(5L, TableStatus.SERVING)); // đã mở hóa đơn
        found(r);

        assertThatThrownBy(() -> service.updateStatus(1L, ReservationStatus.CANCELLED, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hóa đơn");
        assertThat(r.getReservationStatus()).isEqualTo(ReservationStatus.ARRIVED);
    }

    @Test
    void updateStatus_cancelArrived_releasesReservedTable() {
        Reservation r = reservation(1L, ReservationStatus.ARRIVED);
        DiningTable t = table(5L, TableStatus.RESERVED);
        r.setDiningTable(t);
        found(r);

        service.updateStatus(1L, ReservationStatus.CANCELLED, null);

        assertThat(t.getTableStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(r.getReservationStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }


    @Test
    void cancelMine_notOwner_looksLikeNotFound() {
        Reservation r = reservation(1L, ReservationStatus.PENDING);
        Customer owner = new Customer();
        owner.setId("u1");
        r.setCustomer(owner);
        found(r);

        assertThatThrownBy(() -> service.cancelMine(1L, "u2"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(r.getReservationStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void cancelMine_alreadyArrived_throws() {
        Reservation r = reservation(1L, ReservationStatus.ARRIVED);
        Customer owner = new Customer();
        owner.setId("u1");
        r.setCustomer(owner);
        found(r);

        assertThatThrownBy(() -> service.cancelMine(1L, "u1"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateMine_confirmedGoesBackToPending() {
        Reservation r = reservation(1L, ReservationStatus.CONFIRMED);
        Customer owner = new Customer();
        owner.setId("u1");
        r.setCustomer(owner);
        found(r);
        capacity(true, 3, 0);
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateMine(1L, request(), "u1");

        assertThat(r.getReservationStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(r.getConfirmBy()).isNull();
    }


    @Test
    void deleteReservation_onlyPending() {
        found(reservation(1L, ReservationStatus.CONFIRMED));

        assertThatThrownBy(() -> service.deleteReservation(1L))
                .isInstanceOf(IllegalStateException.class);
        verify(reservationRepository, never()).delete(any());
    }
}