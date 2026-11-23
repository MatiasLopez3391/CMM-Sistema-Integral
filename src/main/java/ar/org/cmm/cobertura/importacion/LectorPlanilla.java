package ar.org.cmm.cobertura.importacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Patrón Strategy: un lector por formato de planilla. Para soportar otro formato se agrega una
 * implementación con @Component y @Order; el importador no cambia.
 */
public interface LectorPlanilla {

    /** Indica si este lector sabe leer el archivo (según su nombre). */
    boolean soporta(String nombreArchivo);

    /** Devuelve las filas del archivo como listas de texto. */
    List<List<String>> leer(InputStream in) throws IOException;
}
