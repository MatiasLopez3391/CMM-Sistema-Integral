package ar.org.cmm.ordenes;

import ar.org.cmm.comun.NumeradorService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.comun.UsuarioActualProvider;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.BloqueoServicio;
import ar.org.cmm.contratos.CargoCuotaServicio;
import ar.org.cmm.contratos.CoseguroServicio;
import ar.org.cmm.contratos.ParametroConsulta;
import ar.org.cmm.contratos.PracticaConsulta;
import ar.org.cmm.contratos.PrestadorConsulta;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Emisión y anulación de órdenes (T27, T29, T66).
 *
 * <p>Reglas:
 * <ul>
 *   <li>No se emite a un afiliado dado de baja ni a un grupo bloqueado.</li>
 *   <li>Va a auditoría si alguna práctica lo requiere, si el afiliado supera el tope de órdenes del mes (M11) o si
 *       el médico lo pide (M2).</li>
 *   <li>El coseguro se registra en F3 cuando la orden queda válida: al emitirla si no va a auditoría, o al
 *       autorizarla (ver {@link AuditoriaMedicaService}). Una orden pendiente no genera cargo.</li>
 *   <li>Vence a los N meses de emitida o autorizada (M7). Si no se usó ni se anuló, el coseguro se cobra igual.</li>
 * </ul>
 */
@Service
public class OrdenService {

    static final String TOPE_MENSUAL = "ordenes.tope_mensual";
    static final String MESES_VENCIMIENTO = "ordenes.meses_vencimiento";
    static final Set<String> ESTADOS_QUE_CUENTAN = Set.of(
            OrdenEstado.EMITIDA.name(), OrdenEstado.AUTORIZADA.name(), OrdenEstado.PENDIENTE_AUDITORIA.name());

    private final OrdenRepository ordenRepository;
    private final OrdenEstadoHistorialRepository historialRepository;
    private final AfiliadoConsulta afiliados;
    private final BloqueoServicio bloqueos;
    private final PrestadorConsulta prestadores;
    private final PracticaConsulta practicas;
    private final CoseguroServicio coseguros;
    private final CargoCuotaServicio cargos;
    private final NumeradorService numerador;
    private final UsuarioActualProvider usuarioActual;
    private final ParametroConsulta parametros;

    public OrdenService(
            OrdenRepository ordenRepository,
            OrdenEstadoHistorialRepository historialRepository,
            AfiliadoConsulta afiliados,
            BloqueoServicio bloqueos,
            PrestadorConsulta prestadores,
            PracticaConsulta practicas,
            CoseguroServicio coseguros,
            CargoCuotaServicio cargos,
            NumeradorService numerador,
            UsuarioActualProvider usuarioActual,
            ParametroConsulta parametros) {

        this.ordenRepository = ordenRepository;
        this.historialRepository = historialRepository;
        this.afiliados = afiliados;
        this.bloqueos = bloqueos;
        this.prestadores = prestadores;
        this.practicas = practicas;
        this.coseguros = coseguros;
        this.cargos = cargos;
        this.numerador = numerador;
        this.usuarioActual = usuarioActual;
        this.parametros = parametros;
    }

    /** Calcula la orden sin guardarla: Mesa de Entradas ve el coseguro y si irá a auditoría antes de emitir. */
    @Transactional(readOnly = true)
    public OrdenDtos.SimulacionResponse simular(OrdenDtos.EmitirOrdenRequest request) {
        Calculo c = calcular(request, LocalDate.now());
        List<OrdenDtos.ItemCalculado> items = new ArrayList<>();
        for (int i = 0; i < c.items().size(); i++) {
            OrdenItem it = c.items().get(i);
            var p = c.practicas().get(i);
            items.add(new OrdenDtos.ItemCalculado(p.id(), p.codigoCmm(), p.descripcion(), it.getCantidad(),
                    it.getValorConvenido(), it.getPorcentajeCobertura(), it.getCargoEntidad(), it.getCoseguroAfiliado(),
                    it.isRequiereAuditoria()));
        }
        return new OrdenDtos.SimulacionResponse(items, c.totalValor(), c.totalEntidad(), c.totalCoseguro(),
                c.motivoAuditoria() != null, c.motivoAuditoria(), (int) c.ordenesEnElMes(), c.tope());
    }

    @Transactional
    public OrdenDtos.OrdenResponse emitir(OrdenDtos.EmitirOrdenRequest request) {

        LocalDate fecha = LocalDate.now();
        Calculo c = calcular(request, fecha);
        boolean vaAAuditoria = c.motivoAuditoria() != null;
        OrdenEstado estado = vaAAuditoria ? OrdenEstado.PENDIENTE_AUDITORIA : OrdenEstado.EMITIDA;

        Orden orden = new Orden();
        orden.setAfiliadoId(c.afiliado().id());
        orden.setPrestadorId(c.prestador().id());
        orden.setPlanId(c.afiliado().planId());
        orden.setFechaEmision(fecha);
        orden.setEmitidaPor(usuarioActual.usuario());
        orden.setMedicoSolicitante(recortar(request.medicoSolicitante(), 150));
        orden.setObservaciones(recortar(request.observaciones(), 1000));
        orden.setMotivoAuditoria(vaAAuditoria ? recortar(c.motivoAuditoria(), 300) : null);
        orden.setFechaVencimiento(vaAAuditoria ? null : fecha.plusMonths(mesesVencimiento()));
        c.items().forEach(orden::agregarItem);
        orden.setEstado(estado.name());
        orden.setTotalValorConvenido(c.totalValor());
        orden.setTotalCargoEntidad(c.totalEntidad());
        orden.setTotalCoseguro(c.totalCoseguro());
        orden.setNumero(String.format("ORD-%08d", numerador.siguiente("ORDEN")));

        orden = ordenRepository.saveAndFlush(orden);

        registrarHistorial(orden, null, estado.name(),
                vaAAuditoria ? "Emitida; va a auditoría: " + c.motivoAuditoria() : "Emisión de la orden");

        // Solo una orden válida genera coseguro. La que va a auditoría lo registra cuando se autoriza.
        if (!vaAAuditoria) {
            registrarCoseguro(orden, fecha, c.practicas(), c.prestador().nombre());
        }

        return respuesta(orden);
    }

    /**
     * Anula una orden emitida, autorizada o pendiente de auditoría. Si su coseguro ya se cobró en una cuota no se
     * toca: la respuesta trae el aviso para que Liquidaciones haga la nota de crédito.
     */
    @Transactional
    public OrdenDtos.AnulacionResponse anular(Long ordenId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException("Indique el motivo de la anulación.");
        }
        Orden orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden", ordenId));
        String anterior = orden.getEstado();
        if (!ESTADOS_QUE_CUENTAN.contains(anterior)) {
            throw new ReglaNegocioException("La orden " + orden.getNumero() + " está " + anterior.toLowerCase().replace('_', ' ')
                    + ": no se puede anular.");
        }
        LocalDate hoy = LocalDate.now();
        if (orden.getFechaVencimiento() != null && hoy.isAfter(orden.getFechaVencimiento())) {
            throw new ReglaNegocioException("La orden venció el " + orden.getFechaVencimiento()
                    + ": ya no se puede anular y su coseguro se cobra igual.");
        }
        orden.setEstado(OrdenEstado.ANULADA.name());
        orden.setFechaResolucion(LocalDateTime.now());
        ordenRepository.save(orden);
        registrarHistorial(orden, anterior, OrdenEstado.ANULADA.name(), "Anulada: " + motivo.trim());

        String aviso = null;
        var anulacion = cargos.anularCoseguro(orden.getNumero(), "Orden anulada: " + motivo.trim());
        if (anulacion != null && anulacion.resultado() == CargoCuotaServicio.ResultadoAnulacion.YA_FACTURADO) {
            aviso = "El coseguro de esta orden ya se cobró en la cuota " + periodoLegible(anulacion.periodoCuota())
                    + ". Para devolverlo, Liquidaciones tiene que hacer una nota de crédito.";
        }
        return new OrdenDtos.AnulacionResponse(null, aviso);
    }

    int mesesVencimiento() {
        return parametros.entero(MESES_VENCIMIENTO, 1);
    }

    /** Registra en F3 el coseguro de una orden válida; el concepto nombra las prácticas y el prestador. */
    void registrarCoseguro(Orden orden, LocalDate fecha, List<PracticaConsulta.PracticaResumen> practicasOrden, String prestador) {
        if (orden.getTotalCoseguro() == null || orden.getTotalCoseguro().signum() <= 0) {
            return;
        }
        cargos.registrarCoseguro(new CargoCuotaServicio.CoseguroParaCuota(
                orden.getAfiliadoId(), fecha, orden.getTotalCoseguro(),
                concepto(orden, practicasOrden, prestador), orden.getNumero()));
    }

    static String concepto(Orden orden, List<PracticaConsulta.PracticaResumen> practicasOrden, String prestador) {
        List<String> partes = new ArrayList<>();
        for (int i = 0; i < orden.getItems().size(); i++) {
            OrdenItem it = orden.getItems().get(i);
            if (it.getCantidad() == null || it.getCantidad() <= 0) {
                continue;
            }
            String desc = i < practicasOrden.size() ? practicasOrden.get(i).descripcion() : "Práctica";
            partes.add(it.getCantidad() > 1 ? desc + " x" + it.getCantidad() : desc);
        }
        String texto = String.join(", ", partes) + " - " + prestador + " (" + orden.getNumero() + ")";
        return texto.length() > 200 ? texto.substring(0, 197) + "..." : texto;
    }

    // ------------------------------------------------------------------ Privados

    private record Calculo(
            AfiliadoConsulta.AfiliadoResumen afiliado,
            PrestadorConsulta.PrestadorResumen prestador,
            List<OrdenItem> items,
            List<PracticaConsulta.PracticaResumen> practicas,
            BigDecimal totalValor,
            BigDecimal totalEntidad,
            BigDecimal totalCoseguro,
            String motivoAuditoria,
            long ordenesEnElMes,
            int tope) {
    }

    private Calculo calcular(OrdenDtos.EmitirOrdenRequest request, LocalDate fecha) {

        if (request == null
                || request.afiliadoId() == null
                || request.prestadorId() == null
                || request.items() == null
                || request.items().isEmpty()) {
            throw new ReglaNegocioException("Indique el afiliado, el prestador y al menos una práctica.");
        }

        var afiliado = afiliados.obtener(request.afiliadoId());

        if (!"ACTIVO".equalsIgnoreCase(afiliado.estado())) {
            throw new ReglaNegocioException("El afiliado " + afiliado.nroAfiliado() + " no está activo.");
        }
        if (bloqueos.estaBloqueado(afiliado.id())) {
            throw new ReglaNegocioException("El grupo familiar de " + afiliado.apellidoNombre()
                    + " está bloqueado: no se pueden emitir órdenes hasta que se lo desbloquee.");
        }
        if (afiliado.planId() == null) {
            throw new ReglaNegocioException("El afiliado no tiene un plan asignado.");
        }

        var prestador = prestadores.obtener(request.prestadorId());
        if (!prestador.activo()) {
            throw new ReglaNegocioException("El prestador " + prestador.nombre() + " está inactivo.");
        }

        BigDecimal totalValor = BigDecimal.ZERO;
        BigDecimal totalEntidad = BigDecimal.ZERO;
        BigDecimal totalCoseguro = BigDecimal.ZERO;
        List<OrdenItem> items = new ArrayList<>();
        List<PracticaConsulta.PracticaResumen> practicasOrden = new ArrayList<>();
        List<String> conAuditoria = new ArrayList<>();
        Set<Long> practicasIncluidas = new HashSet<>();

        for (var solicitado : request.items()) {
            if (solicitado == null
                    || solicitado.practicaId() == null
                    || solicitado.cantidad() == null
                    || solicitado.cantidad() <= 0) {
                throw new ReglaNegocioException("Cada práctica necesita una cantidad mayor a cero.");
            }
            if (!practicasIncluidas.add(solicitado.practicaId())) {
                throw new ReglaNegocioException("No se puede repetir una práctica en la misma orden: aumente la cantidad.");
            }

            var practica = practicas.obtener(solicitado.practicaId());
            var calculo = coseguros.calcular(prestador.id(), practica.id(), afiliado.planId(), fecha);
            BigDecimal cantidad = BigDecimal.valueOf(solicitado.cantidad());

            OrdenItem item = new OrdenItem();
            item.setPracticaId(practica.id());
            item.setCantidad(solicitado.cantidad());
            item.setCantidadSolicitada(solicitado.cantidad());
            item.setValorConvenido(calculo.valorConvenido());
            item.setPorcentajeCobertura(calculo.porcentajeCobertura());
            item.setCargoEntidad(calculo.aCargoEntidad().multiply(cantidad));
            item.setCoseguroAfiliado(calculo.coseguroAfiliado().multiply(cantidad));
            item.setRequiereAuditoria(practica.requiereAuditoria());
            items.add(item);
            practicasOrden.add(practica);

            totalValor = totalValor.add(calculo.valorConvenido().multiply(cantidad));
            totalEntidad = totalEntidad.add(item.getCargoEntidad());
            totalCoseguro = totalCoseguro.add(item.getCoseguroAfiliado());
            if (practica.requiereAuditoria()) {
                conAuditoria.add(practica.descripcion());
            }
        }

        // Motivos por los que va a auditoría (puede haber más de uno).
        List<String> motivos = new ArrayList<>();
        if (!conAuditoria.isEmpty()) {
            motivos.add((conAuditoria.size() == 1 ? "la práctica " : "las prácticas ")
                    + String.join(", ", conAuditoria) + (conAuditoria.size() == 1 ? " requiere" : " requieren") + " auditoría");
        }
        int tope = parametros.entero(TOPE_MENSUAL, 3);
        YearMonth mes = YearMonth.from(fecha);
        long enElMes = ordenRepository.countByAfiliadoIdAndFechaEmisionBetweenAndEstadoIn(
                afiliado.id(), mes.atDay(1), mes.atEndOfMonth(), ESTADOS_QUE_CUENTAN);
        if (enElMes >= tope) {
            motivos.add("supera el tope de " + tope + " órdenes en el mes (ya tiene " + enElMes + ")");
        }
        if (Boolean.TRUE.equals(request.enviarAAuditoria())) {
            if (request.motivoAuditoria() == null || request.motivoAuditoria().isBlank()) {
                throw new ReglaNegocioException("Indique por qué el médico pide auditoría.");
            }
            motivos.add("pedido del médico: " + request.motivoAuditoria().trim());
        }
        String motivo = motivos.isEmpty() ? null
                : Character.toUpperCase(motivos.get(0).charAt(0)) + String.join("; ", motivos).substring(1);

        return new Calculo(afiliado, prestador, items, practicasOrden, totalValor, totalEntidad, totalCoseguro,
                motivo, enElMes, tope);
    }

    private void registrarHistorial(Orden orden, String anterior, String nuevo, String comentario) {
        OrdenEstadoHistorial h = new OrdenEstadoHistorial();
        h.setOrden(orden);
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setFecha(LocalDateTime.now());
        h.setUsuario(usuarioActual.usuario());
        h.setComentario(recortar(comentario, 1000));
        historialRepository.save(h);
    }

    static String recortar(String texto, int max) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = texto.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    static String periodoLegible(String periodo) {
        return periodo != null && periodo.length() == 6 ? periodo.substring(4) + "/" + periodo.substring(0, 4) : String.valueOf(periodo);
    }

    private static OrdenDtos.OrdenResponse respuesta(Orden orden) {
        return new OrdenDtos.OrdenResponse(
                orden.getId(),
                orden.getNumero(),
                orden.getAfiliadoId(),
                orden.getPrestadorId(),
                orden.getFechaEmision(),
                orden.getEstado(),
                orden.getTotalValorConvenido(),
                orden.getTotalCargoEntidad(),
                orden.getTotalCoseguro()
        );
    }
}
