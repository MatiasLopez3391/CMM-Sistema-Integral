package ar.org.cmm.cobranza;

import ar.org.cmm.cobranza.AlmacenArchivos.ArchivoGuardado;
import ar.org.cmm.cobranza.CobranzaDtos.CajaDiariaDto;
import ar.org.cmm.cobranza.CobranzaDtos.CajaMedioDto;
import ar.org.cmm.cobranza.CobranzaDtos.LayoutDto;
import ar.org.cmm.cobranza.CobranzaDtos.LoteDto;
import ar.org.cmm.cobranza.CobranzaDtos.MedioDto;
import ar.org.cmm.cobranza.CobranzaDtos.PagoDto;
import ar.org.cmm.cobranza.CobranzaDtos.ResultadoConfirmacion;
import ar.org.cmm.cobranza.LectorArchivoCobro.RegistroLeido;
import ar.org.cmm.comun.AuditoriaService;
import ar.org.cmm.comun.RecursoNoEncontradoException;
import ar.org.cmm.comun.ReglaNegocioException;
import ar.org.cmm.contratos.AfiliadoConsulta;
import ar.org.cmm.contratos.AfiliadoConsulta.AfiliadoResumen;
import ar.org.cmm.contratos.CuentaCorrienteServicio;
import ar.org.cmm.contratos.CuentaCorrienteServicio.PagoParaImputar;
import ar.org.cmm.contratos.CuentaCorrienteServicio.ResultadoImputacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cobranza (H3): importación de archivos .txt de entes de cobro en dos pasos (previsualizar y confirmar)
 * y registro manual de pagos en ventanilla con comprobante adjunto.
 */
@Service
public class CobranzaService {

    private final MedioCobroRepository medios;
    private final LayoutArchivoRepository layouts;
    private final LoteCobroRepository lotes;
    private final PagoRepository pagos;
    private final LectorArchivoCobro lector;
    private final AlmacenArchivos almacen;
    private final AfiliadoConsulta afiliados;
    private final CuentaCorrienteServicio cuentaCorriente;
    private final AuditoriaService auditoria;

    public CobranzaService(MedioCobroRepository medios, LayoutArchivoRepository layouts, LoteCobroRepository lotes,
                           PagoRepository pagos, LectorArchivoCobro lector, AlmacenArchivos almacen, AfiliadoConsulta afiliados,
                           CuentaCorrienteServicio cuentaCorriente, AuditoriaService auditoria) {
        this.medios = medios;
        this.layouts = layouts;
        this.lotes = lotes;
        this.pagos = pagos;
        this.lector = lector;
        this.almacen = almacen;
        this.afiliados = afiliados;
        this.cuentaCorriente = cuentaCorriente;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<MedioDto> medios() {
        return medios.findByActivoTrueOrderByNombreAsc().stream().map(m -> {
            Optional<LayoutArchivo> l = layouts.findFirstByMedioCobroIdAndActivoTrue(m.getId());
            return new MedioDto(m.getId(), m.getCodigo(), m.getNombre(), m.getCuentaContable(), m.isAdmiteArchivo(),
                    l.map(LayoutArchivo::getId).orElse(null), l.map(LayoutArchivo::getNombre).orElse(null),
                    l.map(LayoutArchivo::isProvisorio).orElse(false));
        }).toList();
    }

    // ------------------------------------------------------------------ Importación de archivos

    /** Paso 1: lee el archivo, valida cada registro y deja el lote en PREVISUALIZADO (no impacta en cuentas). */
    @Transactional
    public LoteDto previsualizar(Long medioId, MultipartFile archivo) {
        MedioCobro medio = medios.findById(medioId).orElseThrow(() -> new RecursoNoEncontradoException("Medio de cobro", medioId));
        if (!medio.isAdmiteArchivo()) {
            throw new ReglaNegocioException(medio.getNombre() + " no recibe archivos de cobranza.");
        }
        LayoutArchivo layout = layouts.findFirstByMedioCobroIdAndActivoTrue(medioId)
                .orElseThrow(() -> new ReglaNegocioException("No hay un diseño de archivo configurado para " + medio.getNombre()
                        + ". Configúrelo en Configuración > Diseños de archivo."));
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Seleccione el archivo de cobranza.");
        }
        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer el archivo.");
        }
        String hash = sha256(bytes);
        lotes.findFirstByHashArchivoAndEstado(hash, LoteCobro.CONFIRMADO).ifPresent(l -> {
            throw new ReglaNegocioException("Este archivo ya fue importado y confirmado (lote #" + l.getId() + ").");
        });

        List<RegistroLeido> registros = lector.leer(decodificar(bytes), layout);
        if (registros.isEmpty()) {
            throw new ReglaNegocioException("El archivo no contiene registros de pago con el diseño '" + layout.getNombre() + "'.");
        }
        LoteCobro lote = new LoteCobro();
        lote.setMedioCobro(medio);
        lote.setLayout(layout);
        lote.setNombreArchivo(archivo.getOriginalFilename() == null ? "archivo.txt" : archivo.getOriginalFilename());
        lote.setHashArchivo(hash);
        lote.setEstado(LoteCobro.PREVISUALIZADO);
        lote.setCantidadRegistros(registros.size());
        lote.setImporteOk(BigDecimal.ZERO);
        lotes.save(lote);

        Set<String> referenciasDelArchivo = new HashSet<>();
        int ok = 0, rechazados = 0;
        BigDecimal importeOk = BigDecimal.ZERO;
        for (RegistroLeido r : registros) {
            Pago p = new Pago();
            p.setLote(lote);
            p.setLinea(r.linea());
            p.setMedioCobro(medio);
            p.setNroAfiliadoInformado(r.nroAfiliado());
            p.setFechaPago(r.fecha());
            p.setImporte(r.importe());
            p.setReferencia(r.referencia());
            String rechazo = r.error();
            if (rechazo == null) {
                Optional<AfiliadoResumen> af = afiliados.porNroAfiliado(r.nroAfiliado());
                if (af.isEmpty()) {
                    rechazo = "Afiliado inexistente";
                } else {
                    p.setAfiliadoId(af.get().id());
                    if (r.importe().signum() <= 0) {
                        rechazo = "Importe cero o negativo";
                    } else if (r.fecha().isAfter(LocalDate.now())) {
                        rechazo = "Fecha de pago futura";
                    } else if (r.referencia() != null && !referenciasDelArchivo.add(r.referencia())) {
                        rechazo = "Referencia repetida dentro del archivo";
                    } else if (r.referencia() != null
                            && pagos.existsByMedioCobroIdAndReferenciaAndEstado(medio.getId(), r.referencia(), Pago.IMPUTADO)) {
                        rechazo = "Pago ya registrado anteriormente (referencia " + r.referencia() + ")";
                    }
                }
            }
            if (rechazo == null) {
                p.setEstado(Pago.PENDIENTE);
                ok++;
                importeOk = importeOk.add(r.importe());
            } else {
                p.setEstado(Pago.RECHAZADO);
                p.setMotivoRechazo(rechazo);
                rechazados++;
            }
            pagos.save(p);
        }
        lote.setCantidadOk(ok);
        lote.setCantidadRechazados(rechazados);
        lote.setImporteOk(importeOk);
        return lote(lote.getId());
    }

    /** Paso 2: imputa en cuenta corriente los pagos válidos del lote. */
    @Transactional
    public ResultadoConfirmacion confirmar(Long loteId) {
        LoteCobro lote = lotes.findById(loteId).orElseThrow(() -> new RecursoNoEncontradoException("Lote", loteId));
        if (!LoteCobro.PREVISUALIZADO.equals(lote.getEstado())) {
            throw new ReglaNegocioException("El lote ya está " + lote.getEstado().toLowerCase() + ".");
        }
        lotes.findFirstByHashArchivoAndEstado(lote.getHashArchivo(), LoteCobro.CONFIRMADO).ifPresent(l -> {
            throw new ReglaNegocioException("Este archivo ya fue confirmado en el lote #" + l.getId() + ".");
        });
        int imputados = 0;
        BigDecimal importe = BigDecimal.ZERO, aCuenta = BigDecimal.ZERO;
        List<String> cancelados = new ArrayList<>();
        for (Pago p : pagos.findByLoteIdOrderByLineaAsc(loteId)) {
            if (!Pago.PENDIENTE.equals(p.getEstado())) {
                continue;
            }
            ResultadoImputacion r = cuentaCorriente.registrarPago(new PagoParaImputar(p.getId(), p.getAfiliadoId(), p.getFechaPago(),
                    p.getImporte(), "Pago " + lote.getMedioCobro().getNombre() + (p.getReferencia() != null ? " ref. " + p.getReferencia() : ""),
                    lote.getMedioCobro().getCuentaContable()));
            p.setEstado(Pago.IMPUTADO);
            imputados++;
            importe = importe.add(p.getImporte());
            aCuenta = aCuenta.add(r.aCuenta());
            cancelados.addAll(r.comprobantesCancelados());
        }
        lote.setEstado(LoteCobro.CONFIRMADO);
        lote.setFechaConfirmacion(LocalDateTime.now());
        auditoria.registrar("lote_cobro", loteId, "CONFIRMACION", lote.getNombreArchivo() + ": " + imputados + " pagos, $" + importe);
        return new ResultadoConfirmacion(imputados, importe, aCuenta, cancelados);
    }

    @Transactional
    public void descartar(Long loteId) {
        LoteCobro lote = lotes.findById(loteId).orElseThrow(() -> new RecursoNoEncontradoException("Lote", loteId));
        if (!LoteCobro.PREVISUALIZADO.equals(lote.getEstado())) {
            throw new ReglaNegocioException("Solo se puede descartar un lote en previsualización.");
        }
        lote.setEstado(LoteCobro.DESCARTADO);
        for (Pago p : pagos.findByLoteIdOrderByLineaAsc(loteId)) {
            if (Pago.PENDIENTE.equals(p.getEstado())) {
                p.setEstado(Pago.RECHAZADO);
                p.setMotivoRechazo("Lote descartado");
            }
        }
    }

    @Transactional(readOnly = true)
    public LoteDto lote(Long id) {
        LoteCobro l = lotes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Lote", id));
        List<PagoDto> ps = pagos.findByLoteIdOrderByLineaAsc(id).stream().map(this::pagoDto).toList();
        return loteDto(l, ps);
    }

    @Transactional(readOnly = true)
    public List<LoteDto> lotesRecientes() {
        return lotes.findTop50ByOrderByIdDesc().stream().map(l -> loteDto(l, List.of())).toList();
    }

    // ------------------------------------------------------------------ Ventanilla

    /** Pago en ventanilla con comprobante adjunto (JPG/PNG/PDF). Se imputa en el momento. */
    @Transactional
    public PagoDto registrarPagoManual(Long afiliadoId, LocalDate fecha, BigDecimal importe, String medioCodigo,
                                       String observaciones, MultipartFile comprobante) {
        AfiliadoResumen af = afiliados.obtener(afiliadoId);
        if (importe == null || importe.signum() <= 0) {
            throw new ReglaNegocioException("El importe debe ser mayor a cero.");
        }
        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de pago no puede ser futura.");
        }
        MedioCobro medio = medios.findByCodigo(medioCodigo == null || medioCodigo.isBlank() ? "CAJA" : medioCodigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Medio de cobro", medioCodigo));
        ArchivoGuardado adj = almacen.guardarComprobante(comprobante);
        Pago p = new Pago();
        p.setMedioCobro(medio);
        p.setAfiliadoId(af.id());
        p.setNroAfiliadoInformado(af.nroAfiliado());
        p.setFechaPago(fecha);
        p.setImporte(importe.setScale(2, java.math.RoundingMode.HALF_UP));
        p.setEstado(Pago.IMPUTADO);
        p.setAdjuntoNombre(adj.nombreOriginal());
        p.setAdjuntoRuta(adj.rutaRelativa());
        p.setAdjuntoTipo(adj.tipo());
        p.setObservaciones(observaciones == null || observaciones.isBlank() ? null : observaciones.trim());
        pagos.save(p);
        cuentaCorriente.registrarPago(new PagoParaImputar(p.getId(), af.id(), fecha, p.getImporte(),
                "Pago " + medio.getNombre() + " #" + p.getId(), medio.getCuentaContable()));
        auditoria.registrar("pago", p.getId(), "PAGO_VENTANILLA", af.nroAfiliado() + " $" + p.getImporte());
        return pagoDto(p);
    }

    @Transactional(readOnly = true)
    public List<PagoDto> pagosManualesRecientes() {
        return pagos.findTop100ByLoteIsNullOrderByIdDesc().stream().map(this::pagoDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PagoDto> pagosDeAfiliado(Long afiliadoId) {
        Long titular = afiliados.obtener(afiliadoId).idTitular();
        List<Long> ids = afiliados.integrantesActivos(titular).stream().map(AfiliadoResumen::id).toList();
        List<PagoDto> res = new ArrayList<>();
        for (Long id : ids.isEmpty() ? List.of(titular) : ids) {
            pagos.findByAfiliadoIdAndEstadoOrderByFechaPagoDesc(id, Pago.IMPUTADO).forEach(p -> res.add(pagoDto(p)));
        }
        res.sort((a, b) -> b.fechaPago().compareTo(a.fechaPago()));
        return res;
    }

    @Transactional(readOnly = true)
    public Pago pago(Long id) {
        return pagos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Pago", id));
    }

    public java.nio.file.Path rutaAdjunto(Pago p) {
        if (p.getAdjuntoRuta() == null) {
            throw new RecursoNoEncontradoException("Comprobante adjunto del pago", p.getId());
        }
        return almacen.resolver(p.getAdjuntoRuta());
    }

    // ------------------------------------------------------------------ Caja diaria

    @Transactional(readOnly = true)
    public CajaDiariaDto cajaDiaria(LocalDate fecha) {
        List<Pago> delDia = pagos.imputadosDelDia(fecha);
        Map<String, CajaMedioDto> porMedio = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Pago p : delDia) {
            total = total.add(p.getImporte());
            porMedio.merge(p.getMedioCobro().getNombre(), new CajaMedioDto(p.getMedioCobro().getNombre(), 1, p.getImporte()),
                    (a, b) -> new CajaMedioDto(a.medio(), a.cantidad() + 1, a.total().add(b.total())));
        }
        return new CajaDiariaDto(fecha, total, delDia.size(), new ArrayList<>(porMedio.values()),
                delDia.stream().map(this::pagoDto).toList());
    }

    // ------------------------------------------------------------------ Diseños de archivo

    @Transactional(readOnly = true)
    public List<LayoutDto> layouts() {
        return layouts.findAllByOrderByNombreAsc().stream().map(LayoutDto::de).toList();
    }

    @Transactional
    public LayoutDto guardarLayout(Long id, LayoutDto dto) {
        LayoutArchivo l = layouts.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Diseño de archivo", id));
        if (!LayoutArchivo.ANCHO_FIJO.equals(dto.formato()) && !LayoutArchivo.DELIMITADO.equals(dto.formato())) {
            throw new ReglaNegocioException("Formato inválido (ANCHO_FIJO o DELIMITADO).");
        }
        if (LayoutArchivo.ANCHO_FIJO.equals(dto.formato())
                && (dto.afiliadoLargo() == null || dto.fechaLargo() == null || dto.importeLargo() == null)) {
            throw new ReglaNegocioException("En ancho fijo hay que indicar el largo de afiliado, fecha e importe.");
        }
        try {
            java.time.format.DateTimeFormatter.ofPattern(dto.fechaFormato());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Formato de fecha inválido: " + dto.fechaFormato());
        }
        l.setNombre(dto.nombre());
        l.setFormato(dto.formato());
        l.setDelimitador(dto.delimitador());
        l.setPrefijoRegistro(dto.prefijoRegistro() == null || dto.prefijoRegistro().isBlank() ? null : dto.prefijoRegistro());
        l.setLineasEncabezado(dto.lineasEncabezado());
        l.setAfiliadoPos(dto.afiliadoPos());
        l.setAfiliadoLargo(dto.afiliadoLargo());
        l.setFechaPos(dto.fechaPos());
        l.setFechaLargo(dto.fechaLargo());
        l.setFechaFormato(dto.fechaFormato());
        l.setImportePos(dto.importePos());
        l.setImporteLargo(dto.importeLargo());
        l.setImporteDecimalesImpl(dto.importeDecimalesImpl());
        l.setReferenciaPos(dto.referenciaPos());
        l.setReferenciaLargo(dto.referenciaLargo());
        l.setProvisorio(dto.provisorio());
        l.setObservaciones(dto.observaciones());
        auditoria.registrar("layout_archivo", id, "MODIFICACION", l.getNombre());
        return LayoutDto.de(l);
    }

    // ------------------------------------------------------------------ auxiliares

    private PagoDto pagoDto(Pago p) {
        String nombre = null;
        String nro = p.getNroAfiliadoInformado();
        if (p.getAfiliadoId() != null) {
            AfiliadoResumen a = afiliados.obtener(p.getAfiliadoId());
            nombre = a.apellidoNombre();
            nro = a.nroAfiliado();
        }
        return new PagoDto(p.getId(), p.getLinea(), p.getMedioCobro().getNombre(), p.getAfiliadoId(), nro, nombre, p.getFechaPago(),
                p.getImporte(), p.getReferencia(), p.getEstado(), p.getMotivoRechazo(), p.getAdjuntoRuta() != null,
                p.getAdjuntoNombre(), p.getObservaciones(), p.getCreatedBy(), p.getCreatedAt());
    }

    private static LoteDto loteDto(LoteCobro l, List<PagoDto> ps) {
        return new LoteDto(l.getId(), l.getMedioCobro().getNombre(), l.getNombreArchivo(), l.getEstado(), l.getCantidadRegistros(),
                l.getCantidadOk(), l.getCantidadRechazados(), l.getImporteOk(), l.getCreatedAt(), l.getCreatedBy(),
                l.getFechaConfirmacion(), ps);
    }

    /** Los entes suelen enviar archivos en ISO-8859-1; si no es UTF-8 válido se lee como Latin-1. */
    private static String decodificar(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("ISO-8859-1"));
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
