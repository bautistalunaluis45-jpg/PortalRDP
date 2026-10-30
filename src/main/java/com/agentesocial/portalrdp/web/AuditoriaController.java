package com.agentesocial.portalrdp.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.agentesocial.portalrdp.servicio.ComputadoraService;
import com.agentesocial.portalrdp.servicio.ConsultaAuditoriaService;
import com.agentesocial.portalrdp.servicio.FiltroAuditoria;

/** Pantalla de auditoría (A10). Requiere ROLE_ADMIN. Solo lectura. */
@Controller
@RequestMapping("/web/admin/auditoria")
public class AuditoriaController {

    private final ConsultaAuditoriaService consultaAuditoriaService;
    private final ComputadoraService computadoraService;

    public AuditoriaController(ConsultaAuditoriaService consultaAuditoriaService,
                              ComputadoraService computadoraService) {
        this.consultaAuditoriaService = consultaAuditoriaService;
        this.computadoraService = computadoraService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) Long computadoraId,
                         @RequestParam(required = false) String tipo,
                         @RequestParam(required = false, defaultValue = "7d") String rango,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                         @RequestParam(defaultValue = "0") int pagina,
                         Model model) {
        FiltroAuditoria filtro = new FiltroAuditoria(q, computadoraId, tipo, rango, desde, hasta);
        model.addAttribute("resultado", consultaAuditoriaService.consultar(filtro, pagina));
        model.addAttribute("resumen", consultaAuditoriaService.resumen());
        model.addAttribute("computadoras", computadoraService.listar());
        model.addAttribute("tipos", consultaAuditoriaService.tiposDisponibles());
        model.addAttribute("fq", q);
        model.addAttribute("fComputadoraId", computadoraId);
        model.addAttribute("fTipo", tipo);
        model.addAttribute("fRango", rango);
        model.addAttribute("fDesde", desde);
        model.addAttribute("fHasta", hasta);
        return "web/admin/auditoria";
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportar(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Long computadoraId,
                                           @RequestParam(required = false) String tipo,
                                           @RequestParam(required = false, defaultValue = "7d") String rango,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        String csv = consultaAuditoriaService.exportarCsv(new FiltroAuditoria(q, computadoraId, tipo, rango, desde, hasta));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auditoria.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
