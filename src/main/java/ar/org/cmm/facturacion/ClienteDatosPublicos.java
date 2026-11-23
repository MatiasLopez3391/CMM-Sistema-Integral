package ar.org.cmm.facturacion;

import ar.org.cmm.comun.ReglaNegocioException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Lee datos públicos por HTTP (IPC del INDEC y feriados). Aislado en un componente para poder
 * reemplazarlo en las pruebas y para que un corte de internet se informe con un mensaje claro.
 */
@Component
public class ClienteDatosPublicos {

    private final RestClient http;

    public ClienteDatosPublicos() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(8000);
        f.setReadTimeout(15000);
        this.http = RestClient.builder().requestFactory(f).build();
    }

    public String get(String url, String que) {
        try {
            String cuerpo = http.get().uri(java.net.URI.create(url)).retrieve().body(String.class);
            if (cuerpo == null || cuerpo.isBlank()) {
                throw new ReglaNegocioException("La consulta de " + que + " no devolvió datos.");
            }
            return cuerpo;
        } catch (ReglaNegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new ReglaNegocioException("No se pudo consultar " + que + " (" + e.getClass().getSimpleName()
                    + "). Verifique la conexión a internet del servidor o cárguelo a mano.");
        }
    }
}
