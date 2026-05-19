package com.mby.myStore.Controllers;

import com.mby.myStore.Model.BusinessShift;
import com.mby.myStore.Repositories.BusinessShiftRepository;
import com.mby.myStore.Services.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shifts")
@Tag(name = "Control de horario", description = "Endpoints para modificar el horarío del establecimiento")
public class ShiftController {

    @Autowired
    private ShiftService service;

    @PutMapping("/all")
    @Transactional
    @Operation(summary = "Actualizar horario semanal", description = "Reemplaza todos los turnos de la semana. Requiere rol ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horario actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o solapamiento", content = @Content)
    })
    public ResponseEntity<?> updateAllDaysHours(@RequestBody Map<DayOfWeek, List<BusinessShift>> fullSchedule) {
        service.updateAllSchedule(fullSchedule);
        return ResponseEntity.ok().build();
    }
}