package com.metcoffee.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.metcoffee.dto.DashboardDTO;
import com.metcoffee.service.DashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	// GET /api/admin/dashboard?limiteBajoStock=500 - Ingresos, pedidos, clientes y productos con bajo stock
	@GetMapping
	public ResponseEntity<DashboardDTO> consultar(@RequestParam(defaultValue = "500") double limiteBajoStock) {
		return ResponseEntity.ok(dashboardService.consultar(limiteBajoStock));
	}
}
