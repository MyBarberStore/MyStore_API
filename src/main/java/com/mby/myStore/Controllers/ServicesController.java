package com.mby.myStore.Controllers;

import com.mby.myStore.Model.Service;
import com.mby.myStore.Services.ServicesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/services")
@Tag(name = "Gestión de servicios", description = "CRUD de los servicios que ofrecela barbería")
public class ServicesController {

    @Autowired
    private ServicesService service;

    // OBTENER TODOS
    @GetMapping
    @Operation(summary = "obtener servicios", description = "Recoge todos los servicios.")
    public ResponseEntity<List<Service>> getAll() {
        return ResponseEntity.ok(service.getServicios());
    }

    @PostMapping
    @Operation(summary = "Crear servicio", description = "Registra un nuevo servicio en el catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Servicio creado con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content)
    })
    public ResponseEntity<?> create(@RequestBody Service barberService) {
        try {
            Service created = service.createService(barberService);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar servicio", description = "Elimina un servicio del sistema mediante su ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Servicio eliminado"),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado", content = @Content)
    })
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.deleteService(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar servicio", description = "Modifica los datos de un servicio existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado", content = @Content)
    })
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Service details) {
        try {
            Service updated = service.updateService(id, details);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error interno al actualizar el servicio.");
        }
    }
}