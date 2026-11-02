package com.agentesocial.portalrdp.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import com.agentesocial.portalrdp.infraestructura.PropiedadesHelper;

/**
 * Descarga del helper para instalar en la PC del operador (cualquier usuario autenticado,
 * incluidos empleados). Sirve el .exe de {@code portal.helper.archivo}; 404 si no está publicado.
 */
@Controller
public class DescargaHelperController {

    private final PropiedadesHelper propiedadesHelper;

    public DescargaHelperController(PropiedadesHelper propiedadesHelper) {
        this.propiedadesHelper = propiedadesHelper;
    }

    @GetMapping("/web/helper/descargar")
    public ResponseEntity<Resource> descargar() {
        String ruta = propiedadesHelper.getArchivo();
        if (!StringUtils.hasText(ruta)) {
            return ResponseEntity.notFound().build();
        }
        Path archivo = Path.of(ruta);
        if (!Files.isReadable(archivo) || Files.isDirectory(archivo)) {
            return ResponseEntity.notFound().build();
        }
        long tamano;
        try {
            tamano = Files.size(archivo);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(tamano)
                .body(new FileSystemResource(archivo));
    }
}
