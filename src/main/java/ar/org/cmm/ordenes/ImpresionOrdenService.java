package ar.org.cmm.ordenes;

import ar.org.cmm.comun.reportes.Reporte;
import ar.org.cmm.comun.reportes.Reporte.Tipo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Orden imprimible que Mesa de Entradas le entrega al afiliado (PDF con el formato común de reportes). */
@Service
public class ImpresionOrdenService {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Map<String, String> ESTADOS = Map.of(
            "EMITIDA", "Emitida", "PENDIENTE_AUDITORIA", "Pendiente de auditoría", "AUTORIZADA", "Autorizada",
            "RECHAZADA", "Rechazada", "ANULADA", "Anulada");

    private final OrdenConsultaService consultas;

    public ImpresionOrdenService(OrdenConsultaService consultas) {
        this.consultas = consultas;
    }

    @Transactional(readOnly = true)
    public Reporte orden(Long ordenId) {
        var d = consultas.detalle(ordenId);
        var o = d.orden();
        Reporte.Builder b = Reporte.de("Orden " + o.numero())
                .dato("Afiliado", o.nroAfiliado() + " · " + o.afiliado())
                .dato("Plan", d.planCodigo())
                .dato("Prestador", o.prestador())
                .dato("Emisión", o.fechaEmision().format(F));
        if (o.fechaVencimiento() != null) {
            b.dato("Vence", o.fechaVencimiento().format(F));
        }
        b.dato("Estado", ESTADOS.getOrDefault(o.estado(), o.estado()));
        if (d.medicoSolicitante() != null) {
            b.dato("Médico que prescribe", d.medicoSolicitante());
        }
        b.tabla("Prácticas")
                .columna("Código", Tipo.TEXTO, 1.2f).columna("Descripción", Tipo.TEXTO, 4.5f).columna("Cant.", Tipo.NUMERO, 0.7f)
                .columna("A cargo del CMM", Tipo.MONEDA, 1.6f).columna("Coseguro", Tipo.MONEDA, 1.5f);
        for (var it : d.items()) {
            if (it.cantidad() > 0) {
                b.fila(it.codigo(), it.descripcion(), it.cantidad(), it.cargoEntidad(), it.coseguro());
            }
        }
        b.totales(null, "Total", null, d.totalCargoEntidad(), o.totalCoseguro());
        if (d.observaciones() != null) {
            b.nota("Indicación: " + d.observaciones());
        }
        boolean valida = "EMITIDA".equals(o.estado()) || "AUTORIZADA".equals(o.estado());
        if (!valida) {
            b.nota("Esta orden no es válida para atenderse: está " + ESTADOS.getOrDefault(o.estado(), o.estado()).toLowerCase() + ".");
        } else if (o.vencida()) {
            b.nota("Esta orden está vencida.");
        } else {
            b.nota("El coseguro se cobra en la cuota del mes siguiente. Si no va a usar la orden, anúlela en Mesa de Entradas "
                    + "antes del vencimiento: de lo contrario el coseguro se cobra igual.");
        }
        return b.build();
    }
}
