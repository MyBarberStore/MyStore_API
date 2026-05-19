package com.mby.myStore.Controllers;

import com.mby.myStore.DTO.BillCardsData;
import com.mby.myStore.DTO.InvoiceRequest;
import com.mby.myStore.DTO.InvoiceResponse;
import com.mby.myStore.Model.Invoice;
import com.mby.myStore.Services.InvoicesService;
import com.mby.myStore.Services.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
@Tag(name = "Gestión de facturas", description = "Endpoints CRUD de facturas y exportarlas a pdf")
public class InvoiceController {

    private final InvoicesService invoiceService;
    private final PdfService pdfService;

    // CREAR FACTURA
    @PostMapping
    @Operation(summary = "Crear factura", description = "Registra una nueva factura a partir de la petición.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Factura creada con éxito"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content)
    })
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(request));
    }

    // ACTUALIZAR FACTURA
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar factura", description = "Modifica los datos de una factura existente por su ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Factura actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada", content = @Content)
    })
    public ResponseEntity<InvoiceResponse> update(@PathVariable Long id, @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.updateInvoice(id, request));
    }

    // ELIMINAR FACTURA
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar factura", description = "Elimina una factura del sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Factura eliminada"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada", content = @Content)
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    // OBTENER POR FECHA
    @GetMapping("/by-date/{date}")
    @Operation(summary = "Obtener facturas por fecha", description = "Recupera una lista de facturas emitidas en un día concreto.")
    @ApiResponse(responseCode = "200", description = "Lista de facturas recuperada")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(invoiceService.getInvoicesByDate(date));
    }

    // OBTENER POR ID
    @GetMapping("/{id}")
    @Operation(summary = "Obtener factura por ID", description = "Recupera los detalles de una factura específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Factura encontrada"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada", content = @Content)
    })
    public ResponseEntity<InvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    // EXPORTAR FACTURA A PDF
    @GetMapping(value = "/{id}/export-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Exportar factura a PDF", description = "Genera y descarga el archivo PDF de una factura.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF generado correctamente"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada", content = @Content)
    })
    public ResponseEntity<byte[]> exportToPdf(@PathVariable Long id) {
        Invoice invoice = invoiceService.getInvoiceEntityById(id);
        byte[] pdfBytes = pdfService.generateInvoicePdf(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("factura-" + invoice.getInvoiceNumber() + ".pdf").build());
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // DATOS DE TARJETAS (ESTADÍSTICAS DIARIAS)
    @GetMapping("/billCards/{date}")
    @Operation(summary = "Obtener datos de tarjetas diarias", description = "Calcula los ingresos totales y el número de facturas de un día.")
    @ApiResponse(responseCode = "200", description = "Métricas calculadas con éxito")
    public ResponseEntity<BillCardsData> getBillCards(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        BigDecimal incomes = invoiceService.incomesPerDay(date);
        Integer emittedBills = invoiceService.countByDate(date);
        BillCardsData billCardsData = new BillCardsData();
        billCardsData.setEmittedBills(emittedBills);
        billCardsData.setIncomesPerDay(incomes);
        return ResponseEntity.ok(billCardsData);
    }

    // REPORTE MENSUAL EN PDF
    @GetMapping(value = "/report/monthly", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Generar informe mensual PDF", description = "Crea un informe financiero en PDF basado en el mes y año proporcionados.")
    @ApiResponse(responseCode = "200", description = "Informe mensual generado correctamente")
    public ResponseEntity<byte[]> getMonthlyReport(@RequestParam int month, @RequestParam int year) {
        byte[] pdfContent = pdfService.generateMonthlyReport(month, year);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "informe-" + month + "-" + year + ".pdf");
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    // EXPORTAR FACTURA DESDE CITA (para android)
    @GetMapping(value = "/from-appointment/{id}/export-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Exportar factura desde Cita", description = "Busca la factura vinculada a un ID de cita y genera su PDF.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF de la cita generado"),
            @ApiResponse(responseCode = "404", description = "Cita o factura asociada no encontrada", content = @Content)
    })
    public ResponseEntity<byte[]> getInvoiceFromAppointment(@PathVariable Long id) {
        Invoice invoice = invoiceService.findByAppointmentId(id);
        byte[] pdfContent = pdfService.generateInvoicePdf(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "informe-" + id + ".pdf");
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}