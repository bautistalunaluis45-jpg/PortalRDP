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

import com.agentesocial.portalrdp.infraestructura.PropiedadesAgente;

/**
 * Descarga PUBLICA del agente para instalar en las PCs destino. A diferencia de
 * {@link ComputadorasController#descargarAgente()} (solo admin), esta ruta esta
 * bajo /descargas/** y es permitAll: no requiere login. Pensada para instalar el
 * agente en una PC que todavia no tiene sesion en el portal. El acceso ya queda
 * limitado por la red privada de Tailscale. Sirve el .exe de {@code portal.agente.archivo};
 * 404 si aun no esta publicado.
 */
@Controller
public class DescargaAgenteController {

    private final PropiedadesAgente propiedadesAgente;

    public DescargaAgenteController(PropiedadesAgente propiedadesAgente) {
        this.propiedadesAgente = propiedadesAgente;
    }

    @GetMapping("/descargas/agente")
    public ResponseEntity<Resource> descargarAgente() {
        String ruta = propiedadesAgente.getArchivo();
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
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + archivo.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(tamano)
                .body(new FileSystemResource(archivo));
    }
}
