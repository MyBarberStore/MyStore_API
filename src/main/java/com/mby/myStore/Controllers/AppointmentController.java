package com.mby.myStore.Controllers;

import com.mby.myStore.DTO.AppointmentRequest;
import com.mby.myStore.DTO.AppointmentResponse;
import com.mby.myStore.DTO.DashboardResponse;
import com.mby.myStore.DTO.ServiceCountDTO;
import com.mby.myStore.Model.Service;
import com.mby.myStore.Services.AppointmentService;
import com.mby.myStore.Services.EmployeeService;
import com.mby.myStore.Services.ServicesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@CrossOrigin // Permite peticiones desde la App Android
@Tag(name = "Gestión de Citas", description = "Endpoints para reservar, cancelar, modificar y consultar la agenda")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private ServicesService servicesService;

    /**
     * Crea una nueva cita validando solapamientos.
     *
     * @return 201 Created si tiene éxito o 409 Conflict si el barbero está ocupado.
     */
    @Operation(summary = "Crear nueva cita", description = "Registra una cita validando que el barbero no tenga otra reserva en ese horario.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cita creada con éxito"),
            @ApiResponse(responseCode = "409", description = "Conflicto: El horario ya está ocupado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "Usuario no autenticado")
    })
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@RequestBody AppointmentRequest appointment) {
        AppointmentResponse response = appointmentService.createAppointment(appointment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza una cita existente.
     */
    @Operation(summary = "Actualizar nueva cita", description = "Actualiza los datos de una cita.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cita actualizada correctamente"),
            @ApiResponse(responseCode = "409", description = "Conflicto: El barbero ya tiene una cita en ese horario"),
            @ApiResponse(responseCode = "404", description = "Cita no encontrada"),
            @ApiResponse(responseCode = "403", description = "Usuario no autenticado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> updateAppointment(@PathVariable Long id, @RequestBody AppointmentRequest cita) {

            AppointmentResponse actualizada = appointmentService.updateAppointment(id, cita);
            return ResponseEntity.ok(actualizada);
    }

    @Operation(summary = "Cancelar cita", description = "Cambia el estado de una cita a CANCELLED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cita actualizada correctamente"),
            @ApiResponse(responseCode = "404", description = "Cita no encontrada"),
            @ApiResponse(responseCode = "403", description = "Usuario no autenticado")
    })
    @PutMapping("cancel/{id}")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok().build();
    }


    /**
     * Elimina una cita.
     */
    @Operation(summary = "Eliminar cita", description = "Elimina una cita existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cita eliminada con éxito"),
            @ApiResponse(responseCode = "404", description = "No encontrada: La cita requerida no existe"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "403", description = "Usuario no autenticado")
    })
    @DeleteMapping("/{appointment}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long appointment) {
        appointmentService.deleteAppointment(appointment);
        return ResponseEntity.noContent().build();
    }


    /**
     * Endpoint clave para la App: Devuelve las horas disponibles en tramos de 30min.
     */
    @Operation(summary = "Obtener horas libres",
            description = "Calcula los huecos disponibles de un barbero específico en una fecha determinada, devolviendo tramos de 30 minutos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Éxito"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    @GetMapping("/availability")
    public ResponseEntity<List<LocalTime>> getAvailability(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer excludeId) {

        List<LocalTime> horasLibres = appointmentService.getAvailableHours(employeeId, date, excludeId);
        return ResponseEntity.ok(horasLibres);
    }


    /**
     * Recoge las citas de la fecha introducida
     * @param date
     * @return
     */
    @Operation(summary = "Obtener citas por fecha", description = "Lista las citas de un día específico.")
    @GetMapping
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de citas recuperada (puede estar vacía)"),
            @ApiResponse(responseCode = "400", description = "Fecha inválida o formato incorrecto (debe ser YYYY-MM-DD)"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<AppointmentResponse>> getAppointmentByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AppointmentResponse> dtoViewList= appointmentService.getAppointmentsByDate(date);
        return ResponseEntity.ok(dtoViewList);
    }


    /**
     *  Recoge toda la información necesaria para el dashboard: empleados activos, número de servicios diarios, productos vendidos
     */
    @Operation(
            summary = "Datos estadísticos del Dashboard",
            description = "Recupera la información resumida para el panel: estadísticas de servicios de hoy, empleados activos y productos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del dashboard recuperados con éxito"),
            @ApiResponse(responseCode = "500", description = "Error interno al calcular las estadísticas")
    })
    @GetMapping("/dashboardSummary")
    public ResponseEntity<DashboardResponse> getDailyServices() {
        List<ServiceCountDTO> serviceslist = appointmentService.getTodayServiceStats();
        Long activeEmployees = employeeService.getActiveEmployeesCount();
        int slots = appointmentService.countTotalAvailableSlots(LocalDate.now());

        DashboardResponse dashboardDTO = new DashboardResponse();
        dashboardDTO.setServiceCountDTO(serviceslist);
        dashboardDTO.setEmployeesNumber(activeEmployees);
        dashboardDTO.setAvailableSlots(slots);
        return ResponseEntity.ok(dashboardDTO);
    }


    /**
     * Recoge todos los servicios
     * @return
     */
    @GetMapping("/services")
    @Operation(summary = "Listar servicios de citas", description = "Recupera todo el catálogo de servicios disponibles para el flujo de reservas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de servicios recuperada con éxito"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<Service>> getServices() {
        return ResponseEntity.ok(servicesService.getServicios());
    }

    /**
     * Recoge las citas de un usuario específico
     * @return
     */
    @GetMapping("/user/{id}")
    @Operation(summary = "Obtener citas por Usuario", description = "Recupera el historial completo de citas vinculadas a un ID de cliente específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de citas recuperada con éxito"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<AppointmentResponse>> getUserAppointments(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppoByUserId(id));
    }


}
