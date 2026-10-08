package com.truongtho.be.service;

import com.truongtho.be.dto.request.VoucherRequest;
import com.truongtho.be.dto.response.VoucherResponse;
import com.truongtho.be.entity.Voucher;
import com.truongtho.be.mapper.VoucherMapper;
import com.truongtho.be.repository.VoucherRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level =  AccessLevel.PRIVATE)
public class VoucherService {
    final VoucherRepository voucherRepository;
    final VoucherMapper voucherMapper;


    public Voucher findById(Long idx){
        return  voucherRepository.findById(idx).orElseThrow();
    }
    public VoucherResponse delete(Long idx){
        Voucher voucher = findById(idx);
        voucherRepository.delete(voucher);
        return  voucherMapper.toResponse(voucher);
    }

    public VoucherResponse save(VoucherRequest voucherRequest){
        Voucher voucher = voucherMapper.toEntity(voucherRequest);
        voucherRepository.save(voucher);
        return voucherMapper.toResponse(voucher);
    }

    public VoucherResponse update(Long idx,VoucherRequest voucherRequest){
        Voucher voucher = findById(idx);
        voucher = voucherMapper.toEntity(voucherRequest);
        voucherRepository.save(voucher);
        return voucherMapper.toResponse(voucher);
    }



}
