package com.truongtho.be.controller.admin;

import com.truongtho.be.dto.response.ReindexResponse;
import com.truongtho.be.service.MenuIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/menu-index")
@RequiredArgsConstructor
public class AdminMenuIndexController {
    private final MenuIndexService menuIndexService;

    @PostMapping("/reindex")
    public ReindexResponse reindex() {
        return new ReindexResponse(menuIndexService.reindexAll());
    }
}