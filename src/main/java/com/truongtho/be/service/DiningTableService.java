package com.truongtho.be.service;

import com.truongtho.be.dto.request.DiningTableRequest;
import com.truongtho.be.dto.response.CategoryResponse;
import com.truongtho.be.dto.response.DiningTableResponse;
import com.truongtho.be.entity.DiningTable;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.exception.ResourceNotFoundException;
import com.truongtho.be.mapper.DiningTableMapper;
import com.truongtho.be.repository.DiningTableRepository;
import com.truongtho.be.repository.ReservationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@FieldDefaults(level =  AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class DiningTableService {
    final DiningTableRepository diningTableRepository;
    final DiningTableMapper diningTableMapper;
    final ReservationRepository reservationRepository;




    private DiningTable findById(Long idx){
        return diningTableRepository.findById(idx).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bàn"));
    }

    public DiningTableResponse getById(Long idx){
        DiningTable table = findById(idx);
        return diningTableMapper.toResponse(table);
    }

    public DiningTableResponse delete(Long idx){
        DiningTable table = findById(idx);
        if(reservationRepository.existsByDiningTableId(idx)){
            throw new AppException("Bàn đang có đơn đặt, không thể xóa");
        }
        diningTableRepository.delete(table);
        return  diningTableMapper.toResponse(table);
    }
    public DiningTableResponse save(DiningTableRequest diningTableRequest){
        DiningTable table = diningTableMapper.toEntity(diningTableRequest);
        diningTableRepository.save(table);
        return diningTableMapper.toResponse(table);
    }

    public DiningTableResponse update(Long idx,DiningTableRequest diningTableRequest){
        DiningTable table = findById(idx);
        diningTableMapper.updateEntity(table,diningTableRequest);
        diningTableRepository.save(table);
        return diningTableMapper.toResponse(table);
    }

    public List<DiningTableResponse> getAll(){
        List<DiningTableResponse> diningTableResponses = diningTableRepository.findAll().stream()
                .map(table -> diningTableMapper.toResponse(table))
                .toList();
        return diningTableResponses;
    }

}
